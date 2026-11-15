package nl.jouwapplicatie.respawnBlocks.gui;

import nl.jouwapplicatie.respawnBlocks.RespawnBlocks;
import nl.jouwapplicatie.respawnBlocks.config.BlockSettings;
import nl.jouwapplicatie.respawnBlocks.config.DropMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

/**
 * Edit one respawn block: respawn time, placeholder, drop mode, enabled, remove.
 */
public class BlockEditMenu extends Menu {

    private static final int[] TIME_STEPS = {-60, -10, -1, 0, 1, 10, 60};

    private final Material material;
    private final int returnPage;

    public BlockEditMenu(RespawnBlocks plugin, Material material, int returnPage) {
        super(plugin);
        this.material = material;
        this.returnPage = returnPage;
    }

    @Override
    protected String title() {
        return msg().get("gui.editor.title", "%block%", material.name());
    }

    @Override
    protected int rows() {
        return 5;
    }

    @Override
    protected void build() {
        BlockSettings settings = plugin.blocks().get(material);
        if (settings == null) {
            return; // removed in the meantime
        }

        set(4, Items.of(material, msg().get("gui.blocks.entry-name", "%block%", material.name()), Collections.emptyList()));

        // Time row: -60 -10 -1 [time] +1 +10 +60
        for (int i = 0; i < TIME_STEPS.length; i++) {
            int step = TIME_STEPS[i];
            int slot = 10 + i;
            if (step == 0) {
                set(slot, Items.of(Material.CLOCK,
                        msg().get("gui.editor.time-name", "%time%", msg().formatTime(settings.respawnTime())),
                        msg().list("gui.editor.time-lore")));
                continue;
            }
            Material pane = step > 0 ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
            String name = msg().get("gui.editor.change-name",
                    "%color%", step > 0 ? "a" : "c",
                    "%amount%", (step > 0 ? "+" : "") + step);
            set(slot, Items.of(pane, Math.abs(step), name, Collections.emptyList()), e -> {
                settings.respawnTime(settings.respawnTime() + step);
                plugin.blocks().save();
                refresh();
            });
        }

        // Placeholder
        Material placeholder = plugin.blocks().placeholderFor(settings);
        set(28, Items.of(placeholder, msg().get("gui.editor.placeholder-name", "%placeholder%", placeholder.name()),
                msg().list("gui.editor.placeholder-lore")), e -> {
            if (e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.SHIFT_RIGHT) {
                settings.placeholder(null);
                plugin.blocks().save();
                refresh();
            }
        });

        // Drop mode: DEFAULT -> NATURAL -> INVENTORY -> BLOCK -> DEFAULT
        String drop = settings.dropMode() == null
                ? "DEFAULT (" + plugin.settings().dropMode.name() + ")"
                : settings.dropMode().name();
        set(30, Items.of(Material.HOPPER, msg().get("gui.editor.drop-name", "%drop%", drop), msg().list("gui.editor.drop-lore")), e -> {
            DropMode current = settings.dropMode();
            DropMode next;
            if (current == null) {
                next = DropMode.NATURAL;
            } else if (current.ordinal() + 1 < DropMode.values().length) {
                next = DropMode.values()[current.ordinal() + 1];
            } else {
                next = null;
            }
            settings.dropMode(next);
            plugin.blocks().save();
            refresh();
        });

        // Enabled
        set(32, Items.of(settings.enabled() ? Material.LIME_DYE : Material.GRAY_DYE,
                msg().get("gui.editor.toggle-name", "%state%", msg().state(settings.enabled())),
                msg().list("gui.editor.toggle-lore")), e -> {
            settings.enabled(!settings.enabled());
            plugin.blocks().save();
            refresh();
        });

        // Remove (shift-click to prevent accidents)
        set(34, Items.of(Material.LAVA_BUCKET, msg().get("gui.editor.delete-name"), msg().list("gui.editor.delete-lore")), e -> {
            if (e.isShiftClick()) {
                Player player = (Player) e.getWhoClicked();
                plugin.blocks().remove(material);
                msg().send(player, "block-removed", "%block%", material.name());
                openLater(player, new BlocksMenu(plugin, returnPage));
            }
        });

        set(40, Items.of(Material.OAK_DOOR, msg().get("gui.back"), Collections.emptyList()),
                e -> openLater((Player) e.getWhoClicked(), new BlocksMenu(plugin, returnPage)));

        fillEmpty();
    }

    /** Clicking a block in your own inventory sets it as placeholder. */
    @Override
    public void onBottomClick(Player player, ItemStack clicked, InventoryClickEvent event) {
        BlockSettings settings = plugin.blocks().get(material);
        Material type = clicked.getType();
        if (settings == null) {
            return;
        }
        if (!type.isBlock() || type.isAir()) {
            msg().send(player, "invalid-material", "%input%", type.name());
            return;
        }
        settings.placeholder(type);
        plugin.blocks().save();
        msg().send(player, "block-placeholder-set", "%block%", material.name(), "%placeholder%", type.name());
        refresh();
    }
}
