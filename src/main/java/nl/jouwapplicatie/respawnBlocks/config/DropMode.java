package nl.jouwapplicatie.respawnBlocks.config;

import java.util.Locale;

public enum DropMode {
    /** Vanilla drops on the ground. */
    NATURAL,
    /** Vanilla drops, moved straight into the player's inventory. */
    INVENTORY,
    /** The block itself into the inventory (classic MineIsland). */
    BLOCK;

    public static DropMode parse(String value, DropMode fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
