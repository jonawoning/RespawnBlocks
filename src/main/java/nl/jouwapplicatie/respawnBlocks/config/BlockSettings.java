package nl.jouwapplicatie.respawnBlocks.config;

import org.bukkit.Material;

/**
 * Settings for one respawn block from blocks.yml.
 * placeholder / dropMode are null when the default from config.yml is used.
 */
public class BlockSettings {

    private final Material material;
    private int respawnTime;
    private Material placeholder;
    private DropMode dropMode;
    private boolean enabled;

    public BlockSettings(Material material, int respawnTime, Material placeholder, DropMode dropMode, boolean enabled) {
        this.material = material;
        this.respawnTime = respawnTime;
        this.placeholder = placeholder;
        this.dropMode = dropMode;
        this.enabled = enabled;
    }

    public Material material() {
        return material;
    }

    public int respawnTime() {
        return respawnTime;
    }

    public void respawnTime(int seconds) {
        this.respawnTime = Math.max(1, seconds);
    }

    public Material placeholder() {
        return placeholder;
    }

    public void placeholder(Material placeholder) {
        this.placeholder = placeholder;
    }

    public DropMode dropMode() {
        return dropMode;
    }

    public void dropMode(DropMode dropMode) {
        this.dropMode = dropMode;
    }

    public boolean enabled() {
        return enabled;
    }

    public void enabled(boolean enabled) {
        this.enabled = enabled;
    }
}
