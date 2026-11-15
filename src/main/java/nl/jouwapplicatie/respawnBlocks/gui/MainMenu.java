package nl.jouwapplicatie.respawnBlocks.gui;

import nl.jouwapplicatie.respawnBlocks.RespawnBlocks;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class MainMenu extends Menu {

    public MainMenu(RespawnBlocks plugin) {
        super(plugin);
    }

    @Override
    protected String title() {
        return msg().raw("gui.main.title");
    }

    @Override
    protected int rows() {
        return 3;
    }

    @Override
    protected void build() {
        String blocks = String.valueOf(plugin.blocks().all().size());
        set(10, Items.of(Material.DIAMOND_ORE, msg().get("gui.main.blocks-name"), msg().list("gui.main.blocks-lore", "%amount%", blocks)),
                e -> openLater((Player) e.getWhoClicked(), new BlocksMenu(plugin, 0)));

        String regions = String.valueOf(plugin.regions().count());
        set(12, Items.of(Material.FILLED_MAP, msg().get("gui.main.regions-name"), msg().list("gui.main.regions-lore", "%amount%", regions)),
                e -> {
                    Player player = (Player) e.getWhoClicked();
                    openLater(player, new RegionsMenu(plugin, player.getWorld(), 0, false));
                });

        String pending = String.valueOf(plugin.respawnManager().count());
        set(14, Items.of(Material.CLOCK, msg().get("gui.main.pending-name", "%amount%", pending), msg().list("gui.main.pending-lore")),
                e -> {
                    int amount = plugin.respawnManager().respawnNow(block -> true);
                    msg().send(e.getWhoClicked(), "respawned-all", "%amount%", String.valueOf(amount));
                    refresh();
                });

        boolean enabled = plugin.settings().enabled;
        set(16, Items.of(enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                        msg().get("gui.main.toggle-name", "%state%", msg().state(enabled)), msg().list("gui.main.toggle-lore")),
                e -> {
                    plugin.settings().setEnabled(!plugin.settings().enabled);
                    msg().send(e.getWhoClicked(), plugin.settings().enabled ? "plugin-toggled-on" : "plugin-toggled-off");
                    refresh();
                });

        set(22, Items.of(Material.BARRIER, msg().get("gui.close"), Collections.emptyList()), e -> e.getWhoClicked().closeInventory());

        set(26, Items.of(Material.SUNFLOWER, msg().get("gui.main.reload-name"), msg().list("gui.main.reload-lore")),
                e -> {
                    Player player = (Player) e.getWhoClicked();
                    plugin.reloadAll();
                    msg().send(player, "reloaded");
                    openLater(player, new MainMenu(plugin));
                });

        fillEmpty();
    }
}
