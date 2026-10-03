package dev.kardipopo.superoptimizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Desktop;
import java.nio.file.Path;
import java.util.List;

public final class SuperOptimizerLogScreen extends Screen {
    private final Screen parent;
    private double scroll;
    private double maxScroll;

    public SuperOptimizerLogScreen(Screen parent) {
        super(Component.translatable("superoptimizer.logs.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = this.height - 28;
        addRenderableWidget(Button.builder(Component.translatable("superoptimizer.logs.clear"), b -> {
            SuperOptimizerLog.clear();
        }).bounds(8, y, 100, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("superoptimizer.logs.open_file"), b -> openLogFile())
            .bounds(114, y, 130, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("superoptimizer.logs.refresh"), b -> {
            scroll = 0;
        }).bounds(this.width - 110, y, 100, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> minecraft.gui.setScreen(parent))
            .bounds(this.width / 2 - 80, y, 160, 20).build());
    }

    private void openLogFile() {
        Path path = minecraft.gameDirectory.toPath().resolve("config").resolve("superoptimizer.log");
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(path.toFile());
        } catch (Exception e) {
            SuperOptimizerLog.warn("Не удалось открыть файл логов: " + e.getMessage());
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY >= 36 && mouseY <= this.height - 34) {
            int lines = SuperOptimizerLog.snapshot().size();
            maxScroll = Math.max(0, lines * 11 - (this.height - 78));
            scroll = Math.max(0, Math.min(maxScroll, scroll - verticalAmount * 24));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.text(this.font, this.title, 10, 10, 0xFFFFFFFF, true);
        graphics.text(this.font, Component.translatable("superoptimizer.logs.path"),
            10, 24, 0xFF9FA8B5, false);

        int top = 38;
        int bottom = this.height - 34;
        graphics.enableScissor(0, top, this.width, bottom);

        List<String> lines = SuperOptimizerLog.snapshot();
        int y = top + 4 - (int) scroll;
        for (String line : lines) {
            if (y > top - 12 && y < bottom) {
                graphics.text(this.font, line, 10, y, 0xFFD4D7DC, false);
            }
            y += 11;
        }

        graphics.disableScissor();
    }
}
