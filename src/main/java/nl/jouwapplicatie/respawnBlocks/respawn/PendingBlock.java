package nl.jouwapplicatie.respawnBlocks.respawn;

import org.bukkit.Material;

/**
 * A mined block that is waiting to respawn.
 */
public final class PendingBlock {

    private final BlockPos pos;
    private final String blockData;
    private final Material placeholder;
    private final String region;
    private final long respawnAt;

    /**
     * @param pos         where the block is
     * @param blockData   the original block (BlockData#getAsString, keeps rotation/states)
     * @param placeholder the block that is shown while waiting
     * @param region      the respawn region it was mined in
     * @param respawnAt   wall clock time (ms) at which it respawns, so server downtime counts too
     */
    public PendingBlock(BlockPos pos, String blockData, Material placeholder, String region, long respawnAt) {
        this.pos = pos;
        this.blockData = blockData;
        this.placeholder = placeholder;
        this.region = region;
        this.respawnAt = respawnAt;
    }

    public BlockPos pos() {
        return pos;
    }

    public String blockData() {
        return blockData;
    }

    public Material placeholder() {
        return placeholder;
    }

    public String region() {
        return region;
    }

    public long respawnAt() {
        return respawnAt;
    }

    public boolean isDue(long now) {
        return now >= respawnAt;
    }

    public PendingBlock withRespawnAt(long time) {
        return new PendingBlock(pos, blockData, placeholder, region, time);
    }
}
