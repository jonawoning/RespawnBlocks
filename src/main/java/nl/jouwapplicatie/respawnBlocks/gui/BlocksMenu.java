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
 * Overview of all respawn blocks. Click a block in your own inventory to add it.
 */
public class BlocksMenu extends Menu {

    private static final int PER_PAGE = 45;

    private final int page;

    public BlocksMenu(RespawnBlocks plugin, int page) {
        super(plugin);
        this.page = page;
    }

    private int pages() {
        return Math.max(1, (int) Math.ceil(plugin.blocks().all().size() / (double) PER_PAGE));
    }

    @Override
    protected String title() {
        return msg().get("gui.blocks.title", "%page%", String.valueOf(page + 1), "%pages%", String.valueOf(pages()));
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void build() {
        List<BlockSettings> blocks = plugin.blocks().sorted();
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < blocks.size(); i++) {
            BlockSettings settings = blocks.get(start + i);
            set(i, entry(settings), e -> clickEntry((Player) e.getWhoClicked(), settings, e));
        }

        if (page > 0) {
            set(45, Items.of(Material.ARROW, msg().get("gui.previous-page"), Collections.emptyList()),
                    e -> openLater((Player) e.getWhoClicked(), new BlocksMenu(plugin, page - 1)));
        }
        set(48, Items.of(Material.EMERALD, msg().get("gui.blocks.info-name"), msg().list("gui.blocks.info-lore")));
        set(49, Items.of(Material.OAK_DOOR, msg().get("gui.back"), Collections.emptyList()),
                e -> openLater((Player) e.getWhoClicked(), new MainMenu(plugin)));
        if (page + 1 < pages()) {
            set(53, Items.of(Material.ARROW, msg().get("gui.next-page"), Collections.emptyList()),
                    e -> openLater((Player) e.getWhoClicked(), new BlocksMenu(plugin, page + 1)));
        }
        for (int slot = 45; slot < 54; slot++) {
            if (getInventory().getItem(slot) == null) {
                set(slot, Items.of(Material.BLACK_STAINED_GLASS_PANE, " ", Collections.emptyList()));
            }
        }
    }

    private ItemStack entry(BlockSettings settings) {
        DropMode drop = plugin.blocks().dropModeFor(settings);
        return Items.of(settings.material(), msg().get("gui.blocks.entry-name", "%block%", settings.material().name()),
                msg().list("gui.blocks.entry-lore",
                        "%time%", msg().formatTime(settings.respawnTime()),
                        "%placeholder%", plugin.blocks().placeholderFor(settings).name(),
                        "%drop%", drop.name(),
                        "%state%", msg().state(settings.enabled())));
    }

    private void clickEntry(Player player, BlockSettings settings, InventoryClickEvent event) {
        if (event.getClick() == ClickType.SHIFT_RIGHT) {
            plugin.blocks().remove(settings.material());
            msg().send(player, "block-removed", "%block%", settings.material().name());
            openLater(player, new BlocksMenu(plugin, Math.min(page, Math.max(0, pages() - 1))));
            return;
        }
        openLater(player, new BlockEditMenu(plugin, settings.material(), page));
    }

    @Override
    public void onBottomClick(Player player, ItemStack clicked, InventoryClickEvent event) {
        Material material = clicked.getType();
        if (!material.isBlock() || material.isAir()) {
            msg().send(player, "invalid-material", "%input%", material.name());
            return;
        }
        if (plugin.blocks().contains(material)) {
            openLater(player, new BlockEditMenu(plugin, material, page));
            return;
        }
        int seconds = plugin.settings().defaultRespawnTime;
        plugin.blocks().add(material, seconds);
        msg().send(player, "block-added", "%block%", material.name(), "%time%", msg().formatTime(seconds));
        openLater(player, new BlocksMenu(plugin, page)); // reopen: the page count in the title may change
    }
}
