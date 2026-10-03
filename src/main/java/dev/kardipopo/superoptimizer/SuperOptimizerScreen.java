package dev.kardipopo.superoptimizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class SuperOptimizerScreen extends Screen {
    private final Screen parent;
    private final SuperOptimizerConfig config;

    public SuperOptimizerScreen(Screen parent, SuperOptimizerConfig config) {
        super(Component.translatable("superoptimizer.gui.title"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = 52;

        addRenderableWidget(toggle("superoptimizer.option.enabled", config.enabled, v -> config.enabled = v, cx, y)); y += 24;
        addRenderableWidget(toggle("superoptimizer.option.async", config.asyncPreparation, v -> config.asyncPreparation = v, cx, y)); y += 24;
        addRenderableWidget(toggle("superoptimizer.option.entity", config.entityCulling, v -> config.entityCulling = v, cx, y)); y += 24;
        addRenderableWidget(toggle("superoptimizer.option.block_entity", config.blockEntityCulling, v -> config.blockEntityCulling = v, cx, y)); y += 24;

        addRenderableWidget(Button.builder(Component.translatable("superoptimizer.option.workers", config.workerThreads),
            b -> {
                int max = Math.max(1, Math.min(16, Runtime.getRuntime().availableProcessors() - config.reservedCores));
                config.workerThreads = config.workerThreads >= max ? 1 : config.workerThreads + 1;
                b.setMessage(Component.translatable("superoptimizer.option.workers", config.workerThreads));
            }).bounds(cx - 155, y, 310, 20).build()); y += 24;

        addRenderableWidget(Button.builder(Component.translatable("superoptimizer.option.reserved", config.reservedCores),
            b -> {
                int max = Math.min(8, Math.max(0, Runtime.getRuntime().availableProcessors() - 1));
                config.reservedCores = config.reservedCores >= max ? 0 : config.reservedCores + 1;
                b.setMessage(Component.translatable("superoptimizer.option.reserved", config.reservedCores));
            }).bounds(cx - 155, y, 310, 20).build()); y += 24;

        addRenderableWidget(toggle("superoptimizer.option.pause_motion", config.pauseDuringCameraMotion, v -> config.pauseDuringCameraMotion = v, cx, y)); y += 30;

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> close())
            .bounds(cx - 100, y, 200, 20).build());
    }

    private Button toggle(String key, boolean value, java.util.function.Consumer<Boolean> setter, int cx, int y) {
        final boolean[] state = {value};
        return Button.builder(label(key, state[0]), b -> {
            state[0] = !state[0];
            setter.accept(state[0]);
            b.setMessage(label(key, state[0]));
        }).bounds(cx - 155, y, 310, 20).build();
    }

    private static Component label(String key, boolean value) {
        return Component.translatable(key, value ? "ВКЛ" : "ВЫКЛ");
    }

    private void close() {
        config.save(Minecraft.getInstance().gameDirectory.toPath().resolve("config"));
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}
