package nl.jouwapplicatie.respawnBlocks;

import nl.jouwapplicatie.respawnBlocks.command.RespawnBlocksCommand;
import nl.jouwapplicatie.respawnBlocks.config.BlockRegistry;
import nl.jouwapplicatie.respawnBlocks.config.Messages;
import nl.jouwapplicatie.respawnBlocks.config.Settings;
import nl.jouwapplicatie.respawnBlocks.gui.Menu;
import nl.jouwapplicatie.respawnBlocks.gui.MenuListener;
import nl.jouwapplicatie.respawnBlocks.listener.BlockBreakListener;
import nl.jouwapplicatie.respawnBlocks.listener.WorldListener;
import nl.jouwapplicatie.respawnBlocks.region.RespawnRegions;
import nl.jouwapplicatie.respawnBlocks.respawn.RespawnManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class RespawnBlocks extends JavaPlugin {

    private Settings settings;
    private Messages messages;
    private BlockRegistry blocks;
    private RespawnRegions regions;
    private RespawnManager respawnManager;

    @Override
    public void onEnable() {
        settings = new Settings(this);
        messages = new Messages(this);
        blocks = new BlockRegistry(this);
        regions = new RespawnRegions(this);

        respawnManager = new RespawnManager(this);
        respawnManager.load();
        respawnManager.start();

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new BlockBreakListener(this), this);
        pm.registerEvents(new WorldListener(this), this);
        pm.registerEvents(new MenuListener(), this);

        PluginCommand command = getCommand("respawnblocks");
        if (command != null) {
            RespawnBlocksCommand executor = new RespawnBlocksCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getLogger().info("Enabled with " + blocks.all().size() + " respawn block(s) and "
                + regions.count() + " respawn region(s).");
    }

    @Override
    public void onDisable() {
        // Close open menus so no one keeps a GUI of a disabled plugin
        Menu.closeAll();
        if (respawnManager != null) {
            respawnManager.shutdown();
        }
    }

    /** Reloads config.yml, messages.yml, blocks.yml and regions.yml. Pending respawns are kept. */
    public void reloadAll() {
        settings.reload();
        messages.reload();
        blocks.reload();
        regions.reload();
        respawnManager.start(); // check interval / autosave may have changed
    }

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public BlockRegistry blocks() {
        return blocks;
    }

    public RespawnRegions regions() {
        return regions;
    }

    public RespawnManager respawnManager() {
        return respawnManager;
    }
}
