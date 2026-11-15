package nl.jouwapplicatie.respawnBlocks.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Routes clicks to the open {@link Menu} and makes sure nothing can be taken out of or put into a menu.
 *
 * <p>Uses event.getInventory() (the top inventory) instead of InventoryView, because InventoryView
 * changed from a class to an interface in 1.21 and calling it would break on one side.</p>
 */
public class MenuListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getInventory();
        if (!(top.getHolder() instanceof Menu) || !(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Menu menu = (Menu) top.getHolder();
        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);

        Inventory clicked = event.getClickedInventory();
        if (clicked == null) {
            return;
        }
        if (clicked.equals(top)) {
            menu.handleClick(event);
        } else {
            ItemStack item = event.getCurrentItem();
            if (item != null && !item.getType().isAir()) {
                menu.onBottomClick(player, item, event);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Menu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Menu) {
            Menu.VIEWERS.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Menu.VIEWERS.remove(event.getPlayer().getUniqueId());
    }
}
