package dev.kardipopo.superoptimizer;

import net.minecraft.client.Minecraft;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class ShaderPackCatalog {
    private static final AtomicReference<List<Path>> PACKS = new AtomicReference<>(List.of());

    private ShaderPackCatalog() {}

    public static Path directory(Minecraft client) {
        return client.gameDirectory.toPath().resolve("shaderpacks");
    }

    public static List<Path> snapshot() {
        return PACKS.get();
    }

    public static int fingerprint() {
        return PACKS.get().hashCode();
    }

    public static void refresh(Minecraft client) {
        Runnable scan = () -> {
            List<Path> found = new ArrayList<>();
            Path dir = directory(client);
            try {
                Files.createDirectories(dir);
                try (var stream = Files.list(dir)) {
                    stream.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase().endsWith(".zip"))
                        .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                        .forEach(found::add);
                }
                PACKS.set(List.copyOf(found));
                SuperOptimizerLog.info("Shaderpacks: найдено " + found.size());
                client.execute(() -> {});
            } catch (IOException e) {
                SuperOptimizerLog.warn("Ошибка сканирования shaderpacks: " + e.getMessage());
            }
        };

        if (SuperOptimizerClient.config() != null && SuperOptimizerClient.config().shaderScanAsync
                && SuperOptimizerClient.executor() != null) {
            if (!SuperOptimizerClient.submitBackgroundTask(scan)) {
                SuperOptimizerLog.info("Shaderpack scan отложен Task Backpressure/Frame Budget.");
            }
        } else {
            scan.run();
        }
    }

    public static void importZip(Minecraft client) {
        Thread.startVirtualThread(() -> {
            try {
                FileDialog dialog = new FileDialog((Frame) null, "Импорт shaderpack", FileDialog.LOAD);
                dialog.setFilenameFilter((d, name) -> name.toLowerCase().endsWith(".zip"));
                dialog.setVisible(true);

                String file = dialog.getFile();
                if (file == null) return;

                Path selected = Path.of(dialog.getDirectory(), file);
                Path dir = directory(client);
                Files.createDirectories(dir);
                Path target = dir.resolve(selected.getFileName().toString());
                Files.copy(selected, target, StandardCopyOption.REPLACE_EXISTING);

                client.execute(() -> {
                    SuperOptimizerLog.info("Импортирован shaderpack: " + target.getFileName());
                    refresh(client);
                });
            } catch (Exception e) {
                client.execute(() -> SuperOptimizerLog.warn("Импорт shaderpack отменён/не удался: " + e.getMessage()));
            }
        });
    }

    public static void openFolder(Minecraft client) {
        Path dir = directory(client);
        try {
            Files.createDirectories(dir);
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(dir.toFile());
            }
        } catch (Exception e) {
            SuperOptimizerLog.warn("Не удалось открыть папку shaderpacks: " + e.getMessage());
        }
    }
}
