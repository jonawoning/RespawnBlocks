package nl.jouwapplicatie.respawnBlocks.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Color codes for all versions: '&' codes everywhere, &#RRGGBB hex colors (Minecraft 1.16+).
 * Works with plain String APIs so it runs on both Spigot and Paper.
 */
public final class Text {

    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");

    private Text() {
    }

    public static String color(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        Matcher matcher = HEX.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            // &#a1b2c3 -> §x§a§1§b§2§c§3 (the format the server understands since 1.16)
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : matcher.group(1).toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    /** For item names and lore: starts with a reset so the text is not shown in italics. */
    public static String item(String input) {
        return ChatColor.RESET + color(input);
    }

    public static List<String> itemLines(List<String> lines) {
        List<String> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(item(line));
        }
        return result;
    }

    /** Replaces placeholder pairs: replace(text, "%a%", "1", "%b%", "2"). */
    public static String replace(String text, String... pairs) {
        if (text == null) {
            return "";
        }
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            text = text.replace(pairs[i], pairs[i + 1]);
        }
        return text;
    }

    public static List<String> replace(List<String> lines, String... pairs) {
        List<String> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(replace(line, pairs));
        }
        return result;
    }
}
