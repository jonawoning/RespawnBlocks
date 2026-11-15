package nl.jouwapplicatie.respawnBlocks.config;

import nl.jouwapplicatie.respawnBlocks.RespawnBlocks;
import nl.jouwapplicatie.respawnBlocks.util.YamlFiles;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * blocks.yml: which blocks respawn and how long that takes.
 *
 * <p>Blocks that don't exist on this server version (for example deepslate ores on 1.16) are skipped,
 * but kept in the file, so they work again after updating the server.</p>
 */
public class BlockRegistry {

    private static final List<String> HEADER = Arrays.asList(
            "Respawn blocks. Edit in-game with /rb gui or /rb block ...",
            "",
            "<MATERIAL>:",
            "  respawn-time: <seconds>",
            "  placeholder: <MATERIAL>                (optional, overrides default-placeholder)",
            "  drop-mode: NATURAL|INVENTORY|BLOCK     (optional, overrides drop-mode)",
            "  enabled: true|false                    (optional, default true)",
            "",
            "Blocks that don't exist on your server version are ignored (but kept in this file).");

    private final RespawnBlocks plugin;
    private final File file;
    private final Map<Material, BlockSettings> blocks = new EnumMap<>(Material.class);
    /** Entries for blocks this server version doesn't know, written back unchanged on save. */
    private final Map<String, Object> unknown = new LinkedHashMap<>();

    public BlockRegistry(RespawnBlocks plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "blocks.yml");
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            plugin.saveResource("blocks.yml", false);
        }
        blocks.clear();
        unknown.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("blocks");
        if (section == null) {
            return;
        }
        int skipped = 0;
        int defaultTime = plugin.settings().defaultRespawnTime;
        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            if (material == null || !material.isBlock()) {
                unknown.put(key, section.get(key));
                skipped++;
                continue;
            }
            ConfigurationSection entry = section.getConfigurationSection(key);
            int time = entry == null ? section.getInt(key, defaultTime) // short form "COAL_ORE: 30"
                    : entry.getInt("respawn-time", defaultTime);
            Material placeholder = null;
            DropMode dropMode = null;
            boolean enabled = true;
            if (entry != null) {
                String placeholderName = entry.getString("placeholder");
                if (placeholderName != null) {
                    placeholder = Material.matchMaterial(placeholderName);
                    if (placeholder == null || !placeholder.isBlock() || placeholder.isAir()) {
                        plugin.getLogger().warning("Invalid placeholder '" + placeholderName + "' for " + key + ", using default.");
                        placeholder = null;
                    }
                }
                dropMode = DropMode.parse(entry.getString("drop-mode"), null);
                enabled = entry.getBoolean("enabled", true);
            }
            blocks.put(material, new BlockSettings(material, Math.max(1, time), placeholder, dropMode, enabled));
        }
        if (skipped > 0) {
            plugin.getLogger().info("Skipped " + skipped + " block(s) from blocks.yml that don't exist on this server version.");
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (BlockSettings settings : sorted()) {
            String path = "blocks." + settings.material().name();
            config.set(path + ".respawn-time", settings.respawnTime());
            if (settings.placeholder() != null) {
                config.set(path + ".placeholder", settings.placeholder().name());
            }
            if (settings.dropMode() != null) {
                config.set(path + ".drop-mode", settings.dropMode().name());
            }
            if (!settings.enabled()) {
                config.set(path + ".enabled", false);
            }
        }
        for (Map.Entry<String, Object> entry : unknown.entrySet()) {
            config.set("blocks." + entry.getKey(), entry.getValue());
        }
        try {
            YamlFiles.save(config, file, HEADER);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save blocks.yml: " + e.getMessage());
        }
    }

    /** Settings for this block when it is an enabled respawn block, otherwise null. */
    public BlockSettings active(Material material) {
        BlockSettings settings = blocks.get(material);
        return settings != null && settings.enabled() ? settings : null;
    }

    public BlockSettings get(Material material) {
        return blocks.get(material);
    }

    public boolean contains(Material material) {
        return blocks.containsKey(material);
    }

    public BlockSettings add(Material material, int seconds) {
        BlockSettings settings = new BlockSettings(material, Math.max(1, seconds), null, null, true);
        blocks.put(material, settings);
        save();
        return settings;
    }

    public boolean remove(Material material) {
        boolean removed = blocks.remove(material) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public Collection<BlockSettings> all() {
        return blocks.values();
    }

    public List<BlockSettings> sorted() {
        List<BlockSettings> list = new ArrayList<>(blocks.values());
        list.sort(Comparator.comparing(s -> s.material().name()));
        return list;
    }

    public Material placeholderFor(BlockSettings settings) {
        return settings.placeholder() != null ? settings.placeholder() : plugin.settings().defaultPlaceholder;
    }

    public DropMode dropModeFor(BlockSettings settings) {
        return settings.dropMode() != null ? settings.dropMode() : plugin.settings().dropMode;
    }
}
