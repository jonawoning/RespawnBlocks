package nl.jouwapplicatie.respawnBlocks.region;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import nl.jouwapplicatie.respawnBlocks.RespawnBlocks;
import nl.jouwapplicatie.respawnBlocks.util.YamlFiles;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * regions.yml: which WorldGuard regions are respawn regions (per world) and whether they are enabled.
 */
public class RespawnRegions {

    private final RespawnBlocks plugin;
    private final File file;
    /** world name -> (region id -> enabled) */
    private final Map<String, Map<String, Boolean>> regions = new LinkedHashMap<>();

    public RespawnRegions(RespawnBlocks plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "regions.yml");
        reload();
    }

    public void reload() {
        regions.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("regions");
        if (root == null) {
            return;
        }
        for (String world : root.getKeys(false)) {
            ConfigurationSection worldSection = root.getConfigurationSection(world);
            if (worldSection == null) {
                continue;
            }
            Map<String, Boolean> map = new TreeMap<>();
            for (String id : worldSection.getKeys(false)) {
                map.put(id.toLowerCase(Locale.ROOT), worldSection.getBoolean(id, true));
            }
            regions.put(world, map);
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        regions.forEach((world, map) -> map.forEach((id, enabled) -> config.set("regions." + world + "." + id, enabled)));
        try {
            YamlFiles.save(config, file, Arrays.asList(
                    "Respawn regions per world. Edit in-game with /rb gui or /rb region ...",
                    "regions:",
                    "  <world>:",
                    "    <worldguard region id>: true|false   (enabled)"));
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save regions.yml: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- respawn regions

    public boolean isRespawnRegion(World world, String id) {
        return map(world.getName()).containsKey(id.toLowerCase(Locale.ROOT));
    }

    public boolean isEnabled(World world, String id) {
        return Boolean.TRUE.equals(map(world.getName()).get(id.toLowerCase(Locale.ROOT)));
    }

    public boolean add(World world, String id) {
        Map<String, Boolean> map = regions.computeIfAbsent(world.getName(), w -> new TreeMap<>());
        if (map.putIfAbsent(id.toLowerCase(Locale.ROOT), true) != null) {
            return false;
        }
        save();
        return true;
    }

    public boolean remove(World world, String id) {
        Map<String, Boolean> map = regions.get(world.getName());
        if (map == null || map.remove(id.toLowerCase(Locale.ROOT)) == null) {
            return false;
        }
        if (map.isEmpty()) {
            regions.remove(world.getName());
        }
        save();
        return true;
    }

    public void setEnabled(World world, String id, boolean enabled) {
        Map<String, Boolean> map = regions.get(world.getName());
        if (map != null && map.containsKey(id.toLowerCase(Locale.ROOT))) {
            map.put(id.toLowerCase(Locale.ROOT), enabled);
            save();
        }
    }

    /** Respawn region ids (enabled and disabled) configured for this world. */
    public List<String> respawnRegionIds(World world) {
        return new ArrayList<>(map(world.getName()).keySet());
    }

    public int count() {
        int count = 0;
        for (Map<String, Boolean> map : regions.values()) {
            count += map.size();
        }
        return count;
    }

    private Map<String, Boolean> map(String world) {
        return regions.getOrDefault(world, Collections.emptyMap());
    }

    // ---------------------------------------------------------------- WorldGuard

    /**
     * The id of an enabled respawn region at this location, or null when there is none.
     */
    public String enabledRegionAt(Location location) {
        Map<String, Boolean> map = map(location.getWorld().getName());
        if (map.isEmpty()) {
            return null;
        }
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        ApplicableRegionSet set = container.createQuery().getApplicableRegions(BukkitAdapter.adapt(location));
        for (ProtectedRegion region : set) {
            if (Boolean.TRUE.equals(map.get(region.getId()))) {
                return region.getId();
            }
        }
        return null;
    }

    /** True when the location lies inside the given WorldGuard region. */
    public boolean contains(World world, String id, int x, int y, int z) {
        ProtectedRegion region = worldGuardRegion(world, id);
        return region != null && region.contains(x, y, z);
    }

    public ProtectedRegion worldGuardRegion(World world, String id) {
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        return manager == null ? null : manager.getRegion(id);
    }

    /** All WorldGuard region ids in a world (without __global__), sorted. */
    public List<String> worldGuardRegionIds(World world) {
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (manager == null) {
            return new ArrayList<>();
        }
        List<String> ids = new ArrayList<>();
        for (String id : manager.getRegions().keySet()) {
            if (!id.equalsIgnoreCase("__global__")) {
                ids.add(id);
            }
        }
        Collections.sort(ids);
        return ids;
    }
}
