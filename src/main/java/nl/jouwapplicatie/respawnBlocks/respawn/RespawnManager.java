package nl.jouwapplicatie.respawnBlocks.respawn;

import nl.jouwapplicatie.respawnBlocks.RespawnBlocks;
import nl.jouwapplicatie.respawnBlocks.config.Settings;
import nl.jouwapplicatie.respawnBlocks.util.Compat;
import nl.jouwapplicatie.respawnBlocks.util.YamlFiles;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Keeps track of every block that is waiting to respawn.
 *
 * <p>Chunk safety: a block only respawns while its chunk is loaded. Blocks whose time is up while their
 * chunk is unloaded stay in the list and are respawned the moment the chunk loads again
 * ({@link #onChunkLoad(Chunk)}). Nothing ever force-loads chunks. Everything is saved to data.yml, so
 * placeholders survive restarts and crashes and are still turned back into the original block.</p>
 */
public class RespawnManager {

    /** "BLOCK" since 1.20.5, "BLOCK_CRACK" before that. */
    private static final Particle BLOCK_PARTICLE = Compat.particle("BLOCK", "BLOCK_CRACK");

    private final RespawnBlocks plugin;
    private final File file;

    private final Map<BlockPos, PendingBlock> pending = new HashMap<>();
    private final Map<BlockPos.ChunkPos, Set<BlockPos>> byChunk = new HashMap<>();

    private BukkitTask checkTask;
    private BukkitTask autosaveTask;
    private boolean dirty;

    public RespawnManager(RespawnBlocks plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    // ---------------------------------------------------------------- lifecycle

    public void start() {
        stopTasks();
        Settings settings = plugin.settings();
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, settings.checkIntervalTicks, settings.checkIntervalTicks);
        if (settings.autosaveMinutes > 0) {
            long period = settings.autosaveMinutes * 60L * 20L;
            autosaveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (dirty) {
                    save();
                }
            }, period, period);
        }
    }

    public void stopTasks() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
    }

    public void shutdown() {
        stopTasks();
        if (plugin.settings().respawnLoadedOnShutdown) {
            for (PendingBlock block : new ArrayList<>(pending.values())) {
                if (isChunkLoaded(block.pos())) {
                    restore(block, false);
                }
            }
        }
        save();
    }

    // ---------------------------------------------------------------- tracking

    public void add(PendingBlock block) {
        PendingBlock old = pending.put(block.pos(), block);
        if (old == null) {
            byChunk.computeIfAbsent(block.pos().chunk(), c -> new HashSet<>()).add(block.pos());
        }
        dirty = true;
    }

    public PendingBlock get(Block block) {
        return pending.get(BlockPos.of(block));
    }

    public boolean isPending(Block block) {
        return pending.containsKey(BlockPos.of(block));
    }

    public PendingBlock remove(BlockPos pos) {
        PendingBlock removed = pending.remove(pos);
        if (removed != null) {
            Set<BlockPos> set = byChunk.get(pos.chunk());
            if (set != null) {
                set.remove(pos);
                if (set.isEmpty()) {
                    byChunk.remove(pos.chunk());
                }
            }
            dirty = true;
        }
        return removed;
    }

    public int count() {
        return pending.size();
    }

    public int count(String world, String region) {
        int count = 0;
        for (PendingBlock block : pending.values()) {
            if (block.pos().world().equals(world) && region.equalsIgnoreCase(block.region())) {
                count++;
            }
        }
        return count;
    }

    public Collection<PendingBlock> all() {
        return pending.values();
    }

    // ---------------------------------------------------------------- respawning

    /** Runs every check-interval: respawns every due block whose chunk is loaded. */
    private void tick() {
        if (pending.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        List<PendingBlock> due = new ArrayList<>();
        for (PendingBlock block : pending.values()) {
            if (block.isDue(now) && isChunkLoaded(block.pos())) {
                due.add(block);
            }
        }
        for (PendingBlock block : due) {
            restore(block, true);
        }
    }

    /**
     * Called when a chunk loads. Blocks are restored one tick later, changing blocks while
     * a chunk is still loading is not safe.
     */
    public void onChunkLoad(Chunk chunk) {
        BlockPos.ChunkPos key = new BlockPos.ChunkPos(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        if (!byChunk.containsKey(key)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            Set<BlockPos> positions = byChunk.get(key);
            if (positions == null) {
                return;
            }
            long now = System.currentTimeMillis();
            for (BlockPos pos : new ArrayList<>(positions)) {
                PendingBlock block = pending.get(pos);
                if (block != null && block.isDue(now) && isChunkLoaded(pos)) {
                    restore(block, false);
                }
            }
        });
    }

    /**
     * Respawns matching blocks right away. Blocks in unloaded chunks are marked as due
     * and respawn as soon as their chunk loads.
     *
     * @return the number of affected blocks
     */
    public int respawnNow(Predicate<PendingBlock> filter) {
        int count = 0;
        for (PendingBlock block : new ArrayList<>(pending.values())) {
            if (!filter.test(block)) {
                continue;
            }
            count++;
            if (isChunkLoaded(block.pos())) {
                restore(block, true);
            } else {
                pending.put(block.pos(), block.withRespawnAt(0));
                dirty = true;
            }
        }
        return count;
    }

    /**
     * Puts the original block back. Only replaces the placeholder (or air / liquid): when someone
     * built something else on that spot in the meantime, it is left alone.
     */
    private void restore(PendingBlock block, boolean effects) {
        World world = Bukkit.getWorld(block.pos().world());
        if (world == null) {
            return; // world not loaded, try again later
        }
        remove(block.pos());

        Block target = world.getBlockAt(block.pos().x(), block.pos().y(), block.pos().z());
        if (target.getType() != block.placeholder() && !isReplaceable(target)) {
            return;
        }

        BlockData data;
        try {
            data = Bukkit.createBlockData(block.blockData());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid block data '" + block.blockData() + "' at " + block.pos().serialize());
            return;
        }
        target.setBlockData(data, false);

        if (effects) {
            playEffects(target, data);
        }
    }

    private void playEffects(Block block, BlockData data) {
        Settings settings = plugin.settings();
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        if (settings.respawnSound != null) {
            block.getWorld().playSound(center, settings.respawnSound, settings.respawnVolume, settings.respawnPitch);
        } else if (settings.respawnSoundKey != null) {
            block.getWorld().playSound(center, settings.respawnSoundKey, settings.respawnVolume, settings.respawnPitch);
        }
        if (settings.respawnParticles && BLOCK_PARTICLE != null) {
            try {
                block.getWorld().spawnParticle(BLOCK_PARTICLE, center, 15, 0.35, 0.35, 0.35, 0, data);
            } catch (RuntimeException ignored) {
                // particle data type differs on this version, just skip the effect
            }
        }
    }

    /** Air or a liquid: safe to overwrite with a placeholder or the original block. */
    public static boolean isReplaceable(Block block) {
        Material type = block.getType();
        return type.isAir() || type == Material.WATER || type == Material.LAVA;
    }

    private boolean isChunkLoaded(BlockPos pos) {
        World world = Bukkit.getWorld(pos.world());
        return world != null && world.isChunkLoaded(pos.x() >> 4, pos.z() >> 4);
    }

    // ---------------------------------------------------------------- persistence

    public void load() {
        pending.clear();
        byChunk.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("pending");
        if (section == null) {
            return;
        }
        int loaded = 0;
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            BlockPos pos = BlockPos.deserialize(entry.getString("pos", ""));
            Material placeholder = Material.matchMaterial(entry.getString("placeholder", "BEDROCK"));
            String data = entry.getString("block");
            if (pos == null || placeholder == null || data == null) {
                continue;
            }
            add(new PendingBlock(pos, data, placeholder, entry.getString("region", ""), entry.getLong("respawn-at")));
            loaded++;
        }
        dirty = false;
        if (loaded > 0) {
            plugin.getLogger().info("Loaded " + loaded + " block(s) waiting to respawn.");
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        int i = 0;
        for (PendingBlock block : pending.values()) {
            String path = "pending." + i++;
            config.set(path + ".pos", block.pos().serialize());
            config.set(path + ".block", block.blockData());
            config.set(path + ".placeholder", block.placeholder().name());
            config.set(path + ".region", block.region());
            config.set(path + ".respawn-at", block.respawnAt());
        }
        try {
            YamlFiles.save(config, file, Collections.singletonList("Blocks waiting to respawn. Managed by the plugin, do not edit."));
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }
}
