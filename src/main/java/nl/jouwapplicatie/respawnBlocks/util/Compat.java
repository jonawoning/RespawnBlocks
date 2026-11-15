package nl.jouwapplicatie.respawnBlocks.util;

import org.bukkit.Particle;
import org.bukkit.Sound;

/**
 * Lookups that differ between Minecraft versions.
 *
 * <p>Sound and Particle constants are looked up by name through reflection. That works whether they
 * are enums (older versions) or interfaces with constants (newer versions), and names that only
 * exist on some versions simply return null instead of crashing.</p>
 */
public final class Compat {

    private Compat() {
    }

    public static Sound sound(String name) {
        Object value = constant(Sound.class, name);
        return value instanceof Sound ? (Sound) value : null;
    }

    /** First particle that exists on this server, e.g. particle("BLOCK", "BLOCK_CRACK"). */
    public static Particle particle(String... names) {
        for (String name : names) {
            Object value = constant(Particle.class, name);
            if (value instanceof Particle) {
                return (Particle) value;
            }
        }
        return null;
    }

    private static Object constant(Class<?> type, String name) {
        try {
            return type.getField(name).get(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
