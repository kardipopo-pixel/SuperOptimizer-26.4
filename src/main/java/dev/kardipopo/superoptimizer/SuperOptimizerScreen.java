package dev.kardipopo.superoptimizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
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
        int y = 56;

        this.addRenderableWidget(Button.builder(label("superoptimizer.option.enabled", config.enabled),
                b -> { config.enabled = !config.enabled; b.setMessage(label("superoptimizer.option.enabled", config.enabled)); })
            .bounds(cx - 155, y, 310, 20).build());
        y += 25;

        this.addRenderableWidget(Button.builder(label("superoptimizer.option.async", config.asyncPreparation),
                b -> { config.asyncPreparation = !config.asyncPreparation; b.setMessage(label("superoptimizer.option.async", config.asyncPreparation)); })
            .bounds(cx - 155, y, 310, 20).build());
        y += 25;

        this.addRenderableWidget(Button.builder(label("superoptimizer.option.entity", config.entityCulling),
                b -> { config.entityCulling = !config.entityCulling; b.setMessage(label("superoptimizer.option.entity", config.entityCulling)); })
            .bounds(cx - 155, y, 310, 20).build());
        y += 25;

        this.addRenderableWidget(Button.builder(label("superoptimizer.option.block_entity", config.blockEntityCulling),
                b -> { config.blockEntityCulling = !config.blockEntityCulling; b.setMessage(label("superoptimizer.option.block_entity", config.blockEntityCulling)); })
            .bounds(cx - 155, y, 310, 20).build());
        y += 25;

        this.addRenderableWidget(Button.builder(Component.translatable("superoptimizer.option.workers", config.workerThreads),
                b -> { config.workerThreads = next(config.workerThreads, 1, Math.max(1, Runtime.getRuntime().availableProcessors() / 2)); b.setMessage(Component.translatable("superoptimizer.option.workers", config.workerThreads)); })
            .bounds(cx - 155, y, 310, 20).build());
        y += 25;

        this.addRenderableWidget(Button.builder(Component.translatable("superoptimizer.option.reserved", config.reservedCores),
                b -> { config.reservedCores = next(config.reservedCores, 0, Math.min(8, Runtime.getRuntime().availableProcessors() - 1)); b.setMessage(Component.translatable("superoptimizer.option.reserved", config.reservedCores)); })
            .bounds(cx - 155, y, 310, 20).build());
        y += 25;

        this.addRenderableWidget(Button.builder(label("superoptimizer.option.pause_motion", config.pauseDuringCameraMotion),
                b -> { config.pauseDuringCameraMotion = !config.pauseDuringCameraMotion; b.setMessage(label("superoptimizer.option.pause_motion", config.pauseDuringCameraMotion)); })
            .bounds(cx - 155, y, 310, 20).build());
        y += 30;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                b -> close())
            .bounds(cx - 100, y, 200, 20).build());
    }

    private static int next(int value, int min, int max) {
        return value >= max ? min : value + 1;
    }

    private static Component label(String key, boolean value) {
        return Component.translatable(key, value ? "ВКЛ" : "ВЫКЛ");
    }

    private void close() {
        config.save(Minecraft.getInstance().gameDirectory.toPath().resolve("config"));
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 24, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font,
            Component.translatable("superoptimizer.gui.subtitle"), this.width / 2, 40, 0xFFB0B0B0);
        super.render(graphics, mouseX, mouseY, delta);
    }
}
