package nl.jouwapplicatie.respawnBlocks.config;

import nl.jouwapplicatie.respawnBlocks.RespawnBlocks;
import nl.jouwapplicatie.respawnBlocks.util.Compat;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Typed view of config.yml.
 */
public class Settings {

    private final RespawnBlocks plugin;

    public boolean enabled;
    public Material defaultPlaceholder;
    public int defaultRespawnTime;
    public DropMode dropMode;
    public boolean blockOtherBreaks;
    public boolean overrideProtection;
    public boolean ignoreCreative;
    public int checkIntervalTicks;
    public int autosaveMinutes;
    public boolean respawnLoadedOnShutdown;
    /** Respawn sound as constant (BLOCK_STONE_PLACE). Null when a sound key is used or no sound. */
    public Sound respawnSound;
    /** Respawn sound as key (block.stone.place). Null when a constant is used or no sound. */
    public String respawnSoundKey;
    public float respawnVolume;
    public float respawnPitch;
    public boolean respawnParticles;

    public Settings(RespawnBlocks plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        enabled = c.getBoolean("enabled", true);
        defaultPlaceholder = parseBlock(c.getString("default-placeholder"), Material.BEDROCK);
        defaultRespawnTime = Math.max(1, c.getInt("default-respawn-time", 60));
        dropMode = DropMode.parse(c.getString("drop-mode"), DropMode.INVENTORY);
        blockOtherBreaks = c.getBoolean("block-other-breaks", true);
        overrideProtection = c.getBoolean("override-protection", true);
        ignoreCreative = c.getBoolean("ignore-creative", true);
        checkIntervalTicks = Math.max(1, c.getInt("check-interval-ticks", 20));
        autosaveMinutes = Math.max(0, c.getInt("autosave-minutes", 1));
        respawnLoadedOnShutdown = c.getBoolean("respawn-loaded-on-shutdown", true);

        String soundName = c.getString("respawn-effect.sound", "NONE");
        parseSound(soundName);
        respawnVolume = (float) c.getDouble("respawn-effect.volume", 0.6);
        respawnPitch = (float) c.getDouble("respawn-effect.pitch", 1.0);
        respawnParticles = c.getBoolean("respawn-effect.particles", true);
    }

    /**
     * Changes "enabled" and writes only that line back to config.yml, so the comments in the file
     * are kept (older versions remove all comments when a config is saved through the API).
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        plugin.getConfig().set("enabled", enabled);
        File file = new File(plugin.getDataFolder(), "config.yml");
        try {
            List<String> lines = new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
            boolean found = false;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith("enabled:")) {
                    lines.set(i, "enabled: " + enabled);
                    found = true;
                    break;
                }
            }
            if (!found) {
                lines.add(0, "enabled: " + enabled);
            }
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml: " + e.getMessage());
        }
    }

    private Material parseBlock(String name, Material fallback) {
        Material material = name == null ? null : Material.matchMaterial(name);
        if (material == null || !material.isBlock() || material.isAir()) {
            plugin.getLogger().warning("Invalid block '" + name + "' in config.yml, using " + fallback);
            return fallback;
        }
        return material;
    }

    private void parseSound(String name) {
        respawnSound = null;
        respawnSoundKey = null;
        if (name == null || name.trim().isEmpty() || name.equalsIgnoreCase("NONE")) {
            return;
        }
        // Accept both block.stone.place (sound key, also works for resource pack sounds)
        // and BLOCK_STONE_PLACE (constant name)
        if (name.contains(".") || name.contains(":")) {
            respawnSoundKey = name.toLowerCase(Locale.ROOT);
            return;
        }
        respawnSound = Compat.sound(name.toUpperCase(Locale.ROOT));
        if (respawnSound == null) {
            plugin.getLogger().warning("Unknown sound '" + name + "' for this server version, respawn sound disabled.");
        }
    }
}
