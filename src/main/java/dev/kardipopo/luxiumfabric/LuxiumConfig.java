package dev.kardipopo.luxiumfabric;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class LuxiumConfig {
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("luxium-fabric.properties");
    public static boolean enabled = true;

    private LuxiumConfig() {}

    public static void load() {
        if (!Files.isRegularFile(PATH)) { save(); return; }
        Properties p = new Properties();
        try (var reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
            p.load(reader);
            enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
        } catch (IOException ignored) { enabled = true; }
    }

    private static void save() {
        Properties p = new Properties();
        p.setProperty("enabled", Boolean.toString(enabled));
        try {
            Files.createDirectories(PATH.getParent());
            try (var writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                writer.write("# Luxium Fabric 26.3 private port config\n");
                p.store(writer, null);
            }
        } catch (IOException ignored) {}
    }
}