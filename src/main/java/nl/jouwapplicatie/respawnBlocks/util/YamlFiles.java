package nl.jouwapplicatie.respawnBlocks.util;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public final class YamlFiles {

    private YamlFiles() {
    }

    /**
     * Saves a YAML file with a comment header. The header is written by hand because the header API
     * is different between old and new server versions.
     */
    public static void save(YamlConfiguration config, File file, List<String> header) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String line : header) {
            builder.append(line.isEmpty() ? "#" : "# " + line).append('\n');
        }
        builder.append('\n').append(config.saveToString());
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        Files.write(file.toPath(), builder.toString().getBytes(StandardCharsets.UTF_8));
    }
}
