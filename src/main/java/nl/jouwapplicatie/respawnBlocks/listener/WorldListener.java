package nl.jouwapplicatie.respawnBlocks.listener;

import nl.jouwapplicatie.respawnBlocks.RespawnBlocks;
import nl.jouwapplicatie.respawnBlocks.respawn.RespawnManager;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.List;

/**
 * Chunk handling and protection of placeholders against anything that is not a player.
 */
public class WorldListener implements Listener {

    private final RespawnBlocks plugin;

    public WorldListener(RespawnBlocks plugin) {
        this.plugin = plugin;
    }

    private RespawnManager manager() {
        return plugin.respawnManager();
    }

    /** Respawns blocks whose time ran out while their chunk was unloaded. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        manager().onChunkLoad(event.getChunk());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(manager()::isPending);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(manager()::isPending);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (containsPending(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (containsPending(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (manager().isPending(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    /** Endermen, withers, ravagers, ... */
    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (manager().isPending(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    private boolean containsPending(List<Block> blocks) {
        for (Block block : blocks) {
            if (manager().isPending(block)) {
                return true;
            }
        }
        return false;
    }
}
