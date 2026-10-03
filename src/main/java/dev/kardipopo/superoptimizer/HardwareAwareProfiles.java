package dev.kardipopo.superoptimizer;

import net.minecraft.client.Minecraft;

public final class HardwareAwareProfiles {
    private HardwareAwareProfiles() {}

    public static SuperOptimizerClient.Preset choose() {
        int cores = Runtime.getRuntime().availableProcessors();
        long heapMb = Runtime.getRuntime().maxMemory() / 1048576L;

        int width = 0;
        int height = 0;
        try {
            var window = Minecraft.getInstance().getWindow();
            width = window.getWidth();
            height = window.getHeight();
        } catch (Throwable ignored) {}

        long pixels = Math.max(0L, (long) width * (long) height);

        if (cores <= 4 || heapMb < 4096 || pixels >= 2560L * 1440L) {
            return SuperOptimizerClient.Preset.MICROWAVE;
        }

        if (cores <= 8 || heapMb < 8192 || pixels >= 3840L * 2160L) {
            return SuperOptimizerClient.Preset.ADVANCED;
        }

        return SuperOptimizerClient.Preset.BALANCED;
    }

    public static void applyIfAutomatic() {
        SuperOptimizerConfig c = SuperOptimizerClient.config();
        if (c == null || !c.hardwareAwareProfiles) return;
        if (c.activePreset != null && !c.activePreset.equalsIgnoreCase("AUTO")) return;

        SuperOptimizerClient.Preset preset = choose();
        SuperOptimizerClient.applyPreset(preset);
        SuperOptimizerLog.info("Hardware-Aware Profiles: выбран " + preset.name()
                + " по CPU/heap/разрешению.");
    }
}