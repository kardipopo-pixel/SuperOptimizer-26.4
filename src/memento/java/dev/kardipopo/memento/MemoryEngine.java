package dev.kardipopo.memento;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;

public final class MemoryEngine {
    private static String dimension = "";
    private static String biome = "";
    private static double lastX, lastZ;
    private static float lastHealth = -1;
    private static boolean lastRaining, lastThunder;
    private static long lastCaptureMs, lastBiomeCaptureMs;
    private static boolean armed = true;
    private static boolean deathCaptured;

    private static final long GLOBAL_COOLDOWN = 45_000L;
    private static final long BIOME_COOLDOWN = 5 * 60_000L;

    private MemoryEngine() {}

    public static void reset() {
        dimension = "";
        biome = "";
        lastX = lastZ = 0;
        lastHealth = -1;
        lastRaining = false;
        lastThunder = false;
        lastCaptureMs = 0;
        lastBiomeCaptureMs = 0;
        armed = true;
        deathCaptured = false;
    }

    public static void tick(Minecraft mc) {
        Player player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null || mc.gameRenderer == null || !armed) return;

        String dim = level.dimension().identifier().toString();
        String currentBiome = level.getBiome(player.blockPosition())
                .unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("minecraft:unknown");
        long now = System.currentTimeMillis();

        if (dimension.isEmpty()) {
            dimension = dim;
            biome = currentBiome;
            lastX = player.getX();
            lastZ = player.getZ();
            lastHealth = player.getHealth();
            lastRaining = level.isRaining();
            lastThunder = level.isThundering();
            notifyPlayer("память синхронизирована");
            return;
        }

        if (!dim.equals(dimension)) {
            dimension = dim;
            biome = currentBiome;
            capture(mc, "Смена измерения");
        }

        if (!currentBiome.equals(biome) && now - lastBiomeCaptureMs >= BIOME_COOLDOWN) {
            biome = currentBiome;
            lastBiomeCaptureMs = now;
            capture(mc, "Новый биом");
        } else {
            biome = currentBiome;
        }

        float health = player.getHealth();
        if (lastHealth > 4.0f && health <= 4.0f) capture(mc, "На грани смерти");
        lastHealth = health;

        boolean raining = level.isRaining();
        boolean thunder = level.isThundering();
        if (!lastThunder && thunder) {
            capture(mc, "Началась гроза");
        } else if (!lastRaining && raining && !thunder) {
            capture(mc, "Начался дождь");
        }
        lastRaining = raining;
        lastThunder = thunder;

        double dx = player.getX() - lastX;
        double dz = player.getZ() - lastZ;
        if (dx * dx + dz * dz >= 1_000_000) {
            capture(mc, "Большое путешествие");
            lastX = player.getX();
            lastZ = player.getZ();
        }

        if (player.isDeadOrDying() && !deathCaptured) {
            deathCaptured = true;
            capture(mc, "Смерть");
        }
        if (!player.isDeadOrDying()) deathCaptured = false;
    }

    public static void captureManual(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            notifyPlayer("сейчас нечего сохранять");
            return;
        }
        capture(mc, "Ручной снимок");
    }

    private static void capture(Minecraft mc, String event) {
        long now = System.currentTimeMillis();
        if (!"Смерть".equals(event) && !"Ручной снимок".equals(event)
                && now - lastCaptureMs < GLOBAL_COOLDOWN) return;

        Player p = mc.player;
        ClientLevel level = mc.level;
        if (p == null || level == null) return;

        long id = MemoryStore.nextId();
        String stamp = Instant.ofEpochMilli(now).toString().replaceAll("[^0-9]", "");
        String filename = id + "_" + stamp + ".png";
        String worldName = mc.getCurrentServer() != null ? mc.getCurrentServer().name : "singleplayer";
        String bio = level.getBiome(p.blockPosition()).unwrapKey()
                .map(key -> key.identifier().toString()).orElse("minecraft:unknown");
        String dim = level.dimension().identifier().toString();

        MemoryRecord record = new MemoryRecord(
                id, now, event, worldName, dim, bio,
                p.getX(), p.getY(), p.getZ(), p.getHealth(), filename
        );
        MemoryStore.add(record);

        try {
            Path gameDir = mc.gameDirectory.toPath();
            Screenshot.grab(
                    gameDir.toFile(),
                    "memento/memories/" + filename,
                    mc.gameRenderer.mainRenderTarget(),
                    1,
                    component -> MementoLog.info("Снимок сохранён: " + filename)
            );
            lastCaptureMs = now;
            notifyPlayer(event + "  •  " + record.coordsText());
        } catch (Throwable t) {
            MementoLog.warn("Скриншот не удалось сохранить: " + t.getMessage());
            notifyPlayer("событие сохранено, но снимок не удался");
        }
    }

    private static void notifyPlayer(String message) {
        MementoClient.notifyPlayer(message);
    }

    public static MemoryRecord latest() {
        return MemoryStore.all().stream().findFirst().orElse(null);
    }

    public static String currentContext(Minecraft mc) {
        if (mc.player == null || mc.level == null) return "Нет мира";
        String b = mc.level.getBiome(mc.player.blockPosition()).unwrapKey()
                .map(k -> k.identifier().getPath()).orElse("unknown");
        return String.format(Locale.ROOT, "%s • %s • %.0f, %.0f, %.0f",
                mc.level.dimension().identifier(), b,
                mc.player.getX(), mc.player.getY(), mc.player.getZ());
    }

    public static String mood(Minecraft mc) {
        if (mc.player == null || mc.level == null) return "ВНЕ МИРА";
        if (mc.player.getHealth() <= 4) return "ОПАСНОСТЬ";
        if (mc.level.isThundering()) return "ГРОЗА";
        if (mc.level.isRaining()) return "ДОЖДЬ";
        long time = mc.level.getGameTime() % 24000L;
        if (time >= 13000 && time <= 23000) return "НОЧЬ";
        return "СПОКОЙНО";
    }

    public static boolean isArmed() { return armed; }

    public static void setArmed(boolean value) { armed = value; }
}
