package dev.kardipopo.superoptimizer;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Method;

public final class IrisBridge {
    private IrisBridge() {}

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded("iris");
    }

    public static boolean openMainScreen(Screen parent) {
        if (!isLoaded()) return false;

        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Method getInstance = apiClass.getMethod("getInstance");
            Object api = getInstance.invoke(null);

            Method open = apiClass.getMethod("openMainIrisScreenObj", Object.class);
            Object screen = open.invoke(api, parent);

            if (screen instanceof Screen irisScreen) {
                Minecraft.getInstance().setScreenAndShow(irisScreen);
                SuperOptimizerLog.info("Открыт штатный экран Iris.");
                return true;
            }

            SuperOptimizerLog.warn("Iris API вернул неизвестный экран.");
        } catch (ReflectiveOperationException | LinkageError e) {
            SuperOptimizerLog.warn("Не удалось открыть экран Iris: " + e);
        }

        return false;
    }

    public static boolean shadersInUse() {
        if (!isLoaded()) return false;

        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            return (boolean) apiClass.getMethod("isShaderPackInUse").invoke(api);
        } catch (ReflectiveOperationException | LinkageError e) {
            return false;
        }
    }
}
