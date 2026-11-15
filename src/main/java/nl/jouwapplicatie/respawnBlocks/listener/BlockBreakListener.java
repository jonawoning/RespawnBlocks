package nl.jouwapplicatie.respawnBlocks.listener;

import nl.jouwapplicatie.respawnBlocks.RespawnBlocks;
import nl.jouwapplicatie.respawnBlocks.config.BlockSettings;
import nl.jouwapplicatie.respawnBlocks.config.DropMode;
import nl.jouwapplicatie.respawnBlocks.config.Settings;
import nl.jouwapplicatie.respawnBlocks.respawn.BlockPos;
import nl.jouwapplicatie.respawnBlocks.respawn.PendingBlock;
import nl.jouwapplicatie.respawnBlocks.respawn.RespawnManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Handles breaking blocks inside respawn regions.
 *
 * <p>The break itself is NOT cancelled: vanilla handles drops, fortune, silk touch, tool durability,
 * experience and statistics, and other plugins (jobs, mcMMO, ...) see a normal break.
 * One tick later the empty spot is filled with the placeholder.</p>
 */
public class BlockBreakListener implements Listener {

    public static final String BYPASS = "respawnblocks.bypass";

    private final RespawnBlocks plugin;

    /** Decision taken at HIGHEST, executed at MONITOR when no other plugin cancelled the break. */
    private Decision decision;
    /** Blocks whose vanilla drops must go into the breaker's inventory. */
    private final Map<BlockPos, Player> inventoryDrops = new HashMap<>();
    /** Blocks that get their placeholder next tick. */
    private final Set<BlockPos> awaitingPlaceholder = new HashSet<>();

    public BlockBreakListener(RespawnBlocks plugin) {
        this.plugin = plugin;
    }

    private static final class Decision {
        final BlockPos pos;
        final BlockSettings settings;
        final String region;
        final String blockData;

        Decision(BlockPos pos, BlockSettings settings, String region, String blockData) {
            this.pos = pos;
            this.settings = settings;
            this.region = region;
            this.blockData = blockData;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreakDecide(BlockBreakEvent event) {
        decision = null;
        Settings settings = plugin.settings();
        if (!settings.enabled) {
            return;
        }
        Player player = event.getPlayer();
        Block block = event.getBlock();
        RespawnManager manager = plugin.respawnManager();

        // A placeholder that is waiting to respawn can't be mined.
        PendingBlock pending = manager.get(block);
        if (pending != null || awaitingPlaceholder.contains(BlockPos.of(block))) {
            if (player.getGameMode() == GameMode.CREATIVE && player.hasPermission(BYPASS)) {
                manager.remove(BlockPos.of(block)); // admin removes it for good
                return;
            }
            event.setCancelled(true);
            return;
        }

        if (settings.ignoreCreative && player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        String region = plugin.regions().enabledRegionAt(block.getLocation());
        if (region == null) {
            return;
        }

        BlockSettings blockSettings = plugin.blocks().active(block.getType());
        if (blockSettings == null) {
            if (settings.blockOtherBreaks && !event.isCancelled() && !player.hasPermission(BYPASS)) {
                event.setCancelled(true);
                plugin.messages().send(player, "cannot-break");
            }
            return;
        }

        if (event.isCancelled()) {
            if (!settings.overrideProtection) {
                return;
            }
            event.setCancelled(false);
        }
        decision = new Decision(BlockPos.of(block), blockSettings, region, block.getBlockData().getAsString());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakApply(BlockBreakEvent event) {
        Decision current = decision;
        decision = null;
        Block block = event.getBlock();
        if (current == null || !current.pos.equals(BlockPos.of(block))) {
            return;
        }
        Player player = event.getPlayer();
        BlockSettings settings = current.settings;
        Material placeholder = plugin.blocks().placeholderFor(settings);
        long respawnAt = System.currentTimeMillis() + settings.respawnTime() * 1000L;

        plugin.respawnManager().add(new PendingBlock(current.pos, current.blockData, placeholder, current.region, respawnAt));

        DropMode mode = plugin.blocks().dropModeFor(settings);
        if (mode == DropMode.BLOCK && block.getType().isItem()) {
            event.setDropItems(false);
            giveOrDrop(player, new ItemStack(block.getType()), block);
            giveExp(player, event);
        } else if (mode == DropMode.INVENTORY) {
            inventoryDrops.put(current.pos, player);
            giveExp(player, event);
        }

        awaitingPlaceholder.add(current.pos);
        World world = block.getWorld();
        BlockPos pos = current.pos;
        Bukkit.getScheduler().runTask(plugin, () -> placePlaceholder(world, pos, placeholder));
    }

    /** Moves the vanilla drops into the inventory (INVENTORY drop mode). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(BlockDropItemEvent event) {
        Player player = inventoryDrops.remove(BlockPos.of(event.getBlock()));
        if (player == null) {
            return;
        }
        Iterator<Item> iterator = event.getItems().iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next();
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.getItemStack());
            if (leftover.isEmpty()) {
                iterator.remove();
            } else {
                item.setItemStack(leftover.values().iterator().next());
            }
        }
    }

    private void placePlaceholder(World world, BlockPos pos, Material placeholder) {
        awaitingPlaceholder.remove(pos);
        inventoryDrops.remove(pos);
        if (plugin.respawnManager().get(world.getBlockAt(pos.x(), pos.y(), pos.z())) == null) {
            return; // already respawned or removed
        }
        Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
        if (RespawnManager.isReplaceable(block)) {
            block.setType(placeholder, false);
        } else {
            // Something else ended up there (e.g. a falling block). Don't overwrite it.
            plugin.respawnManager().remove(pos);
        }
    }

    private void giveOrDrop(Player player, ItemStack item, Block block) {
        for (ItemStack leftover : player.getInventory().addItem(item).values()) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), leftover);
        }
    }

    /**
     * Drops the experience as an orb at the player instead of at the block, so it is picked up right away
     * (and repairs mending items, like normal orbs do).
     */
    private void giveExp(Player player, BlockBreakEvent event) {
        int exp = event.getExpToDrop();
        if (exp > 0) {
            event.setExpToDrop(0);
            ExperienceOrb orb = player.getWorld().spawn(player.getLocation(), ExperienceOrb.class);
            orb.setExperience(exp);
        }
    }
}
