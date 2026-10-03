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

    private static Object api() throws ReflectiveOperationException {
        Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
        Method getInstance = apiClass.getMethod("getInstance");
        return getInstance.invoke(null);
    }

    public static boolean openMainScreen(Screen parent) {
        if (!isLoaded()) return false;

        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = api();

            Object screen = apiClass.getMethod("openMainIrisScreenObj", Object.class)
                .invoke(api, parent);

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

    public static boolean shadersEnabled() {
        if (!isLoaded()) return false;

        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = api();
            Object cfg = apiClass.getMethod("getConfig").invoke(api);
            return (boolean) cfg.getClass().getMethod("areShadersEnabled").invoke(cfg);
        } catch (ReflectiveOperationException | LinkageError e) {
            return false;
        }
    }

    public static boolean setShadersEnabled(boolean enabled) {
        if (!isLoaded()) return false;

        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = api();
            Object cfg = apiClass.getMethod("getConfig").invoke(api);
            cfg.getClass().getMethod("setShadersEnabledAndApply", boolean.class).invoke(cfg, enabled);
            SuperOptimizerLog.info("Iris: шейдеры " + (enabled ? "включены" : "выключены") + ".");
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            SuperOptimizerLog.warn("Не удалось изменить состояние Iris: " + e);
            return false;
        }
    }

    public static boolean shadersInUse() {
        if (!isLoaded()) return false;

        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = api();
            return (boolean) apiClass.getMethod("isShaderPackInUse").invoke(api);
        } catch (ReflectiveOperationException | LinkageError e) {
            return false;
        }
    }
}
