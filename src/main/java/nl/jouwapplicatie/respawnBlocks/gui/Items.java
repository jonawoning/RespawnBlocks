package nl.jouwapplicatie.respawnBlocks.gui;

import nl.jouwapplicatie.respawnBlocks.util.Text;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class Items {

    private Items() {
    }

    public static ItemStack of(Material material, String name, List<String> lore) {
        return of(material, 1, name, lore);
    }

    public static ItemStack of(Material material, int amount, String name, List<String> lore) {
        // Some blocks (fire, water, ...) have no item form.
        Material icon = material.isItem() && !material.isAir() ? material : Material.PAPER;
        ItemStack item = new ItemStack(icon, Math.max(1, Math.min(amount, icon.getMaxStackSize())));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Text.item(name));
        meta.setLore(Text.itemLines(lore));
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }
}
