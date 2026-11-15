package nl.jouwapplicatie.respawnBlocks.respawn;

import org.bukkit.block.Block;

import java.util.Objects;

/**
 * Immutable block position. Uses the world name (not a Location) so it is safe as a map key
 * and does not keep a world loaded in memory.
 */
public final class BlockPos {

    private final String world;
    private final int x;
    private final int y;
    private final int z;

    public BlockPos(String world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static BlockPos of(Block block) {
        return new BlockPos(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public String world() {
        return world;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public ChunkPos chunk() {
        return new ChunkPos(world, x >> 4, z >> 4);
    }

    public String serialize() {
        return world + ";" + x + ";" + y + ";" + z;
    }

    public static BlockPos deserialize(String input) {
        String[] parts = input.split(";");
        if (parts.length != 4) {
            return null;
        }
        try {
            return new BlockPos(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BlockPos)) {
            return false;
        }
        BlockPos other = (BlockPos) o;
        return x == other.x && y == other.y && z == other.z && world.equals(other.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, y, z);
    }

    @Override
    public String toString() {
        return serialize();
    }

    /** Chunk coordinates. */
    public static final class ChunkPos {

        private final String world;
        private final int x;
        private final int z;

        public ChunkPos(String world, int x, int z) {
            this.world = world;
            this.x = x;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ChunkPos)) {
                return false;
            }
            ChunkPos other = (ChunkPos) o;
            return x == other.x && z == other.z && world.equals(other.world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(world, x, z);
        }
    }
}
