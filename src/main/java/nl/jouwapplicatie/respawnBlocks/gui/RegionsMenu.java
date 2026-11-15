package nl.jouwapplicatie.respawnBlocks.gui;

import nl.jouwapplicatie.respawnBlocks.RespawnBlocks;
import nl.jouwapplicatie.respawnBlocks.region.RespawnRegions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

/**
 * All WorldGuard regions of a world. Click to turn them into respawn regions, toggle or remove them.
 */
public class RegionsMenu extends Menu {

    private static final int PER_PAGE = 45;

    private final World world;
    private final int page;
    private final boolean respawnOnly;

    public RegionsMenu(RespawnBlocks plugin, World world, int page, boolean respawnOnly) {
        super(plugin);
        this.world = world;
        this.page = page;
        this.respawnOnly = respawnOnly;
    }

    /** WorldGuard regions + respawn regions that no longer exist in WorldGuard. */
    private List<String> entries() {
        RespawnRegions regions = plugin.regions();
        TreeSet<String> ids = new TreeSet<>(regions.respawnRegionIds(world));
        if (!respawnOnly) {
            ids.addAll(regions.worldGuardRegionIds(world));
        }
        return new ArrayList<>(ids);
    }

    private int pages(int amount) {
        return Math.max(1, (int) Math.ceil(amount / (double) PER_PAGE));
    }

    @Override
    protected String title() {
        return msg().get("gui.regions.title", "%world%", world.getName(),
                "%page%", String.valueOf(page + 1), "%pages%", String.valueOf(pages(entries().size())));
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void build() {
        List<String> ids = entries();
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < ids.size(); i++) {
            String id = ids.get(start + i);
            set(i, entry(id), e -> clickEntry((Player) e.getWhoClicked(), id, e));
        }

        if (page > 0) {
            set(45, Items.of(Material.ARROW, msg().get("gui.previous-page"), Collections.emptyList()),
                    e -> openLater((Player) e.getWhoClicked(), new RegionsMenu(plugin, world, page - 1, respawnOnly)));
        }
        set(47, Items.of(worldIcon(world), msg().get("gui.regions.world-name", "%world%", world.getName()),
                msg().list("gui.regions.world-lore")), e -> openLater((Player) e.getWhoClicked(),
                new RegionsMenu(plugin, nextWorld(), 0, respawnOnly)));
        set(49, Items.of(Material.OAK_DOOR, msg().get("gui.back"), Collections.emptyList()),
                e -> openLater((Player) e.getWhoClicked(), new MainMenu(plugin)));
        String filter = msg().raw(respawnOnly ? "gui.regions.filter-respawn" : "gui.regions.filter-all");
        set(51, Items.of(Material.HOPPER, msg().get("gui.regions.filter-name", "%filter%", filter),
                msg().list("gui.regions.filter-lore")), e -> openLater((Player) e.getWhoClicked(),
                new RegionsMenu(plugin, world, 0, !respawnOnly)));
        if (page + 1 < pages(ids.size())) {
            set(53, Items.of(Material.ARROW, msg().get("gui.next-page"), Collections.emptyList()),
                    e -> openLater((Player) e.getWhoClicked(), new RegionsMenu(plugin, world, page + 1, respawnOnly)));
        }
        for (int slot = 45; slot < 54; slot++) {
            if (getInventory().getItem(slot) == null) {
                set(slot, Items.of(Material.BLACK_STAINED_GLASS_PANE, " ", Collections.emptyList()));
            }
        }
    }

    private ItemStack entry(String id) {
        RespawnRegions regions = plugin.regions();
        String name = msg().get("gui.regions.entry-name", "%region%", id);
        if (regions.worldGuardRegion(world, id) == null) {
            return Items.of(Material.BARRIER, name, msg().list("gui.regions.entry-lore-missing"));
        }
        if (!regions.isRespawnRegion(world, id)) {
            return Items.of(Material.WHITE_WOOL, name, msg().list("gui.regions.entry-lore-normal"));
        }
        boolean enabled = regions.isEnabled(world, id);
        return Items.of(enabled ? Material.LIME_WOOL : Material.RED_WOOL, name, msg().list("gui.regions.entry-lore-respawn",
                "%state%", msg().state(enabled),
                "%amount%", String.valueOf(plugin.respawnManager().count(world.getName(), id))));
    }

    private void clickEntry(Player player, String id, InventoryClickEvent event) {
        RespawnRegions regions = plugin.regions();
        String[] repl = {"%region%", id, "%world%", world.getName()};

        if (!regions.isRespawnRegion(world, id)) {
            if (regions.add(world, id)) {
                msg().send(player, "region-added", repl);
            }
            refresh();
            return;
        }
        if (event.getClick() == ClickType.SHIFT_RIGHT) {
            regions.remove(world, id);
            msg().send(player, "region-removed", repl);
            // Reopen: with the "respawn only" filter the page count may change
            openLater(player, new RegionsMenu(plugin, world, page, respawnOnly));
            return;
        }
        if (regions.worldGuardRegion(world, id) == null) {
            return; // missing regions can only be removed
        }
        boolean enabled = !regions.isEnabled(world, id);
        regions.setEnabled(world, id, enabled);
        msg().send(player, enabled ? "region-enabled" : "region-disabled", repl);
        refresh();
    }

    private World nextWorld() {
        List<World> worlds = Bukkit.getWorlds();
        int index = worlds.indexOf(world);
        return worlds.get((index + 1) % worlds.size());
    }

    private static Material worldIcon(World world) {
        switch (world.getEnvironment()) {
            case NETHER:
                return Material.NETHERRACK;
            case THE_END:
                return Material.END_STONE;
            default:
                return Material.GRASS_BLOCK;
        }
    }
}
