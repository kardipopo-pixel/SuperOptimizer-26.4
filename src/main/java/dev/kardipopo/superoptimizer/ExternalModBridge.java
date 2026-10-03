package dev.kardipopo.superoptimizer;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Method;

/** Optional integrations. We open other mods' own config screens instead of duplicating their state. */
public final class ExternalModBridge {
    private ExternalModBridge() {}

    public static boolean loaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static String version(String modId) {
        try {
            return FabricLoader.getInstance().getModContainer(modId)
                    .map(c -> c.getMetadata().getVersion().getFriendlyString())
                    .orElse("не установлен");
        } catch (Throwable e) {
            return "неизвестно";
        }
    }

    public static boolean openConfig(String modId, Screen parent) {
        // Mod Menu exposes the canonical config factory for third-party mods.
        if (loaded("modmenu")) {
            try {
                Class<?> menu = Class.forName("com.terraformersmc.modmenu.ModMenu");
                Method getConfig = menu.getMethod("getConfigScreen", String.class, Screen.class);
                Object screen = getConfig.invoke(null, modId, parent);
                if (screen instanceof Screen s) {
                    Minecraft.getInstance().setScreenAndShow(s);
                    return true;
                }
            } catch (ReflectiveOperationException | LinkageError e) {
                SuperOptimizerLog.warn("Не удалось открыть меню " + modId + " через Mod Menu: " + e.getMessage());
            }
        }

        if ("iris".equals(modId)) {
            return IrisBridge.openMainScreen(parent);
        }

        if ("sodium".equals(modId)) {
            return openSodiumFallback(parent);
        }

        SuperOptimizerLog.warn("У " + modId + " нет доступного публичного экрана конфигурации.");
        return false;
    }

    private static boolean openSodiumFallback(Screen parent) {
        String[] names = {
                "net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI",
                "me.jellysquid.mods.sodium.client.gui.SodiumOptionsGUI"
        };
        for (String name : names) {
            try {
                Class<?> clazz = Class.forName(name);
                java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(Screen.class);
                Object screen = ctor.newInstance(parent);
                if (screen instanceof Screen s) {
                    Minecraft.getInstance().setScreenAndShow(s);
                    return true;
                }
            } catch (ReflectiveOperationException | LinkageError ignored) {}
        }
        return false;
    }
}