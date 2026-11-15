package nl.jouwapplicatie.respawnBlocks.gui;

import nl.jouwapplicatie.respawnBlocks.RespawnBlocks;
import nl.jouwapplicatie.respawnBlocks.config.Messages;
import nl.jouwapplicatie.respawnBlocks.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Base class for all GUIs. A menu is the holder of its own inventory, so clicks can be routed back
 * to it by {@link MenuListener}. Items are never taken out of a menu.
 */
public abstract class Menu implements InventoryHolder {

    /** Players that currently have a menu open (used to close them when the plugin disables). */
    static final Set<UUID> VIEWERS = new HashSet<>();

    protected final RespawnBlocks plugin;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();
    private Inventory inventory;

    protected Menu(RespawnBlocks plugin) {
        this.plugin = plugin;
    }

    protected abstract String title();

    protected abstract int rows();

    /** Fills the inventory. Called on open and on every {@link #refresh()}. */
    protected abstract void build();

    /** Click on an item in the player's own inventory while this menu is open. */
    public void onBottomClick(Player player, ItemStack clicked, InventoryClickEvent event) {
    }

    public void open(Player player) {
        inventory = Bukkit.createInventory(this, rows() * 9, Text.color(title()));
        build();
        player.openInventory(inventory);
        VIEWERS.add(player.getUniqueId()); // after openInventory: that closes (and unregisters) the old menu first
    }

    /** Closes every open menu. */
    public static void closeAll() {
        for (UUID uuid : new HashSet<>(VIEWERS)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.closeInventory();
            }
        }
        VIEWERS.clear();
    }

    /** Opens another menu one tick later (opening inventories inside a click event is unsafe). */
    protected void openLater(Player player, Menu menu) {
        Bukkit.getScheduler().runTask(plugin, () -> menu.open(player));
    }

    public void refresh() {
        inventory.clear();
        actions.clear();
        build();
    }

    void handleClick(InventoryClickEvent event) {
        Consumer<InventoryClickEvent> action = actions.get(event.getRawSlot());
        if (action != null) {
            action.accept(event);
        }
    }

    protected void set(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        inventory.setItem(slot, item);
        if (action != null) {
            actions.put(slot, action);
        } else {
            actions.remove(slot);
        }
    }

    protected void set(int slot, ItemStack item) {
        set(slot, item, null);
    }

    /** Fills empty slots with glass panes. */
    protected void fillEmpty() {
        ItemStack filler = Items.of(Material.GRAY_STAINED_GLASS_PANE, " ", Collections.emptyList());
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    protected Messages msg() {
        return plugin.messages();
    }

    protected int size() {
        return rows() * 9;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
