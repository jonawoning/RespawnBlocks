package nl.jouwapplicatie.respawnBlocks.command;

import nl.jouwapplicatie.respawnBlocks.RespawnBlocks;
import nl.jouwapplicatie.respawnBlocks.config.BlockSettings;
import nl.jouwapplicatie.respawnBlocks.config.Messages;
import nl.jouwapplicatie.respawnBlocks.gui.MainMenu;
import nl.jouwapplicatie.respawnBlocks.region.RespawnRegions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * /respawnblocks (aliases /rb, /mineisland)
 */
public class RespawnBlocksCommand implements TabExecutor {

    private static final String ADMIN = "respawnblocks.admin";
    private static final String RELOAD = "respawnblocks.reload";

    private static final List<String> SUBCOMMANDS = Arrays.asList("help", "gui", "region", "block", "respawnall", "toggle", "reload");
    private static final List<String> REGION_ACTIONS = Arrays.asList("add", "remove", "toggle", "list");
    private static final List<String> BLOCK_ACTIONS = Arrays.asList("add", "remove", "settime", "list");
    private static final List<String> TIME_SUGGESTIONS = Arrays.asList("30", "60", "120", "300", "600");

    private final RespawnBlocks plugin;

    public RespawnBlocksCommand(RespawnBlocks plugin) {
        this.plugin = plugin;
    }

    private Messages msg() {
        return plugin.messages();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "gui" : args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("reload")) {
            if (!sender.hasPermission(RELOAD) && !sender.hasPermission(ADMIN)) {
                msg().send(sender, "no-permission");
                return true;
            }
            plugin.reloadAll();
            msg().send(sender, "reloaded");
            return true;
        }

        if (!sender.hasPermission(ADMIN)) {
            msg().send(sender, "no-permission");
            return true;
        }

        switch (sub) {
            case "gui":
            case "menu":
                if (sender instanceof Player) {
                    new MainMenu(plugin).open((Player) sender);
                } else if (args.length == 0) {
                    sendHelp(sender, label);
                } else {
                    msg().send(sender, "players-only");
                }
                break;
            case "region":
            case "regions":
                region(sender, label, args);
                break;
            case "block":
            case "blocks":
                block(sender, label, args);
                break;
            case "respawnall":
                respawnAll(sender, args);
                break;
            case "toggle": {
                boolean enabled = !plugin.settings().enabled;
                plugin.settings().setEnabled(enabled);
                msg().send(sender, enabled ? "plugin-toggled-on" : "plugin-toggled-off");
                break;
            }
            case "help":
                sendHelp(sender, label);
                break;
            default:
                msg().send(sender, "unknown-command", "%label%", label);
        }
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        for (String line : msg().list("help", "%label%", label)) {
            msg().sendPlain(sender, line);
        }
        msg().send(sender, "pending-info", "%amount%", String.valueOf(plugin.respawnManager().count()));
    }

    // ---------------------------------------------------------------- /rb region

    private void region(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            msg().send(sender, "usage", "%usage%", "/" + label + " region <add|remove|toggle|list> <region> [world]");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        RespawnRegions regions = plugin.regions();

        if (action.equals("list")) {
            World world = resolveWorld(sender, args, 2);
            if (world == null) {
                return;
            }
            List<String> ids = regions.respawnRegionIds(world);
            if (ids.isEmpty()) {
                msg().send(sender, "region-list-empty", "%world%", world.getName());
                return;
            }
            msg().send(sender, "region-list-header", "%world%", world.getName());
            for (String id : ids) {
                msg().sendPlain(sender, msg().get("region-list-entry", "%region%", id,
                        "%state%", msg().state(regions.isEnabled(world, id))));
            }
            return;
        }

        if (args.length < 3) {
            msg().send(sender, "usage", "%usage%", "/" + label + " region " + action + " <region> [world]");
            return;
        }
        String id = args[2].toLowerCase(Locale.ROOT);
        World world = resolveWorld(sender, args, 3);
        if (world == null) {
            return;
        }
        String[] repl = {"%region%", id, "%world%", world.getName()};

        switch (action) {
            case "add":
            case "define":
                if (regions.worldGuardRegion(world, id) == null) {
                    msg().send(sender, "region-not-found", repl);
                } else if (!regions.add(world, id)) {
                    msg().send(sender, "region-already", repl);
                } else {
                    msg().send(sender, "region-added", repl);
                }
                break;
            case "remove":
            case "delete":
                if (regions.remove(world, id)) {
                    msg().send(sender, "region-removed", repl);
                } else {
                    msg().send(sender, "region-not-respawn", repl);
                }
                break;
            case "toggle":
            case "enable":
            case "disable": {
                if (!regions.isRespawnRegion(world, id)) {
                    msg().send(sender, "region-not-respawn", repl);
                    return;
                }
                boolean enabled = action.equals("toggle") ? !regions.isEnabled(world, id) : action.equals("enable");
                regions.setEnabled(world, id, enabled);
                msg().send(sender, enabled ? "region-enabled" : "region-disabled", repl);
                break;
            }
            default:
                msg().send(sender, "usage", "%usage%", "/" + label + " region <add|remove|toggle|list> <region> [world]");
        }
    }

    /** World from args[index], otherwise the player's own world (or the first world for the console). */
    private World resolveWorld(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            World world = Bukkit.getWorld(args[index]);
            if (world == null) {
                msg().send(sender, "world-not-found", "%world%", args[index]);
            }
            return world;
        }
        if (sender instanceof Player) {
            return ((Player) sender).getWorld();
        }
        return Bukkit.getWorlds().get(0);
    }

    // ---------------------------------------------------------------- /rb block

    private void block(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            msg().send(sender, "usage", "%usage%", "/" + label + " block <add|remove|settime|list> <block> [seconds]");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);

        if (action.equals("list")) {
            msg().send(sender, "block-list-header");
            for (BlockSettings settings : plugin.blocks().sorted()) {
                msg().sendPlain(sender, msg().get("block-list-entry",
                        "%block%", settings.material().name(),
                        "%time%", msg().formatTime(settings.respawnTime()),
                        "%placeholder%", plugin.blocks().placeholderFor(settings).name()));
            }
            return;
        }

        if (args.length < 3) {
            msg().send(sender, "usage", "%usage%", "/" + label + " block " + action + " <block>" + (action.equals("remove") ? "" : " <seconds>"));
            return;
        }
        Material material = parseMaterial(sender, args[2]);
        if (material == null) {
            return;
        }
        String name = material.name();

        switch (action) {
            case "add": {
                if (plugin.blocks().contains(material)) {
                    msg().send(sender, "block-already", "%block%", name);
                    return;
                }
                int seconds = plugin.settings().defaultRespawnTime;
                if (args.length > 3) {
                    Integer parsed = parseSeconds(sender, args[3]);
                    if (parsed == null) {
                        return;
                    }
                    seconds = parsed;
                }
                plugin.blocks().add(material, seconds);
                msg().send(sender, "block-added", "%block%", name, "%time%", msg().formatTime(seconds));
                break;
            }
            case "remove": {
                if (plugin.blocks().remove(material)) {
                    msg().send(sender, "block-removed", "%block%", name);
                } else {
                    msg().send(sender, "block-not-respawn", "%block%", name);
                }
                break;
            }
            case "settime":
            case "time": {
                BlockSettings settings = plugin.blocks().get(material);
                if (settings == null) {
                    msg().send(sender, "block-not-respawn", "%block%", name);
                    return;
                }
                if (args.length < 4) {
                    msg().send(sender, "usage", "%usage%", "/" + label + " block settime <block> <seconds>");
                    return;
                }
                Integer seconds = parseSeconds(sender, args[3]);
                if (seconds == null) {
                    return;
                }
                settings.respawnTime(seconds);
                plugin.blocks().save();
                msg().send(sender, "block-time-set", "%block%", name, "%time%", msg().formatTime(seconds));
                break;
            }
            default:
                msg().send(sender, "usage", "%usage%", "/" + label + " block <add|remove|settime|list> <block> [seconds]");
        }
    }

    private Material parseMaterial(CommandSender sender, String input) {
        if (input.equalsIgnoreCase("hand")) {
            if (!(sender instanceof Player)) {
                msg().send(sender, "players-only");
                return null;
            }
            Player player = (Player) sender;
            Material hand = player.getInventory().getItemInMainHand().getType();
            if (!hand.isBlock() || hand.isAir()) {
                msg().send(sender, "hold-a-block");
                return null;
            }
            return hand;
        }
        Material material = Material.matchMaterial(input);
        if (material == null || !material.isBlock() || material.isAir()) {
            msg().send(sender, "invalid-material", "%input%", input);
            return null;
        }
        return material;
    }

    private Integer parseSeconds(CommandSender sender, String input) {
        try {
            int seconds = Integer.parseInt(input);
            if (seconds > 0) {
                return seconds;
            }
        } catch (NumberFormatException ignored) {
            // handled below
        }
        msg().send(sender, "invalid-number", "%input%", input);
        return null;
    }

    // ---------------------------------------------------------------- /rb respawnall

    private void respawnAll(CommandSender sender, String[] args) {
        int amount;
        if (args.length > 1) {
            String region = args[1];
            amount = plugin.respawnManager().respawnNow(block -> region.equalsIgnoreCase(block.region()));
        } else {
            amount = plugin.respawnManager().respawnNow(block -> true);
        }
        msg().send(sender, "respawned-all", "%amount%", String.valueOf(amount));
    }

    // ---------------------------------------------------------------- tab completion

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(ADMIN)) {
            return args.length == 1 && sender.hasPermission(RELOAD) ? filter(Collections.singletonList("reload"), args[0]) : Collections.<String>emptyList();
        }
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "region":
            case "regions":
                return completeRegion(sender, args);
            case "block":
            case "blocks":
                return completeBlock(args);
            case "respawnall":
                return args.length == 2 ? filter(allRespawnRegionIds(), args[1]) : Collections.<String>emptyList();
            default:
                return Collections.emptyList();
        }
    }

    private List<String> completeRegion(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return filter(REGION_ACTIONS, args[1]);
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("list")) {
            return args.length == 3 ? filter(worldNames(), args[2]) : Collections.<String>emptyList();
        }
        if (args.length == 3) {
            World world = sender instanceof Player ? ((Player) sender).getWorld() : Bukkit.getWorlds().get(0);
            List<String> options = action.equals("add")
                    ? notYetRespawn(world)
                    : plugin.regions().respawnRegionIds(world);
            return filter(options, args[2]);
        }
        if (args.length == 4) {
            return filter(worldNames(), args[3]);
        }
        return Collections.<String>emptyList();
    }

    private List<String> completeBlock(String[] args) {
        if (args.length == 2) {
            return filter(BLOCK_ACTIONS, args[1]);
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("list")) {
            return Collections.<String>emptyList();
        }
        if (args.length == 3) {
            List<String> options = new ArrayList<>();
            if (action.equals("add")) {
                options.add("hand");
                for (Material material : Material.values()) {
                    if (material.isBlock() && !material.isAir() && !material.name().startsWith("LEGACY_") && !plugin.blocks().contains(material)) {
                        options.add(material.name().toLowerCase(Locale.ROOT));
                    }
                }
            } else {
                for (BlockSettings settings : plugin.blocks().sorted()) {
                    options.add(settings.material().name().toLowerCase(Locale.ROOT));
                }
            }
            return filter(options, args[2]);
        }
        if (args.length == 4 && (action.equals("add") || action.equals("settime"))) {
            return filter(TIME_SUGGESTIONS, args[3]);
        }
        return Collections.<String>emptyList();
    }

    private List<String> notYetRespawn(World world) {
        List<String> ids = new ArrayList<>();
        for (String id : plugin.regions().worldGuardRegionIds(world)) {
            if (!plugin.regions().isRespawnRegion(world, id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    private List<String> allRespawnRegionIds() {
        List<String> ids = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            for (String id : plugin.regions().respawnRegionIds(world)) {
                if (!ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private List<String> worldNames() {
        List<String> names = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            names.add(world.getName());
        }
        return names;
    }

    private static List<String> filter(List<String> options, String input) {
        List<String> result = new ArrayList<>();
        StringUtil.copyPartialMatches(input, options, result);
        return result;
    }
}
