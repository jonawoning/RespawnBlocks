package nl.jouwapplicatie.respawnBlocks.config;

import nl.jouwapplicatie.respawnBlocks.RespawnBlocks;
import nl.jouwapplicatie.respawnBlocks.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * messages.yml: prefix, chat messages and all GUI texts.
 * Missing keys fall back to the defaults inside the jar, so updating the plugin never breaks old files.
 */
public class Messages {

    private final RespawnBlocks plugin;
    private YamlConfiguration config;

    public Messages(RespawnBlocks plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        try (Reader reader = new InputStreamReader(plugin.getResource("messages.yml"), StandardCharsets.UTF_8)) {
            config.setDefaults(YamlConfiguration.loadConfiguration(reader));
        } catch (Exception e) {
            plugin.getLogger().warning("Could not load default messages: " + e.getMessage());
        }
    }

    public String prefix() {
        return config.getString("prefix", "");
    }

    public String raw(String key) {
        return config.getString(key, key);
    }

    public List<String> list(String key) {
        return config.getStringList(key);
    }

    public String get(String key, String... replacements) {
        return Text.replace(raw(key), replacements);
    }

    public List<String> list(String key, String... replacements) {
        return Text.replace(list(key), replacements);
    }

    /** Sends a message with prefix. Empty messages are not sent. */
    public void send(CommandSender sender, String key, String... replacements) {
        String message = get(key, replacements);
        if (message.isEmpty()) {
            return;
        }
        sender.sendMessage(Text.color(prefix() + message));
    }

    /** Sends a message without prefix. */
    public void sendPlain(CommandSender sender, String message) {
        if (!message.isEmpty()) {
            sender.sendMessage(Text.color(message));
        }
    }

    public String state(boolean enabled) {
        return raw(enabled ? "state-enabled" : "state-disabled");
    }

    /** 3725 -> "1h 2m 5s" using the configurable time format. */
    public String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        StringBuilder builder = new StringBuilder();
        if (hours > 0) {
            builder.append(raw("time.hours").replace("%h%", String.valueOf(hours)));
        }
        if (minutes > 0) {
            builder.append(raw("time.minutes").replace("%m%", String.valueOf(minutes)));
        }
        if (seconds > 0 || builder.length() == 0) {
            builder.append(raw("time.seconds").replace("%s%", String.valueOf(seconds)));
        }
        return builder.toString().trim();
    }
}
