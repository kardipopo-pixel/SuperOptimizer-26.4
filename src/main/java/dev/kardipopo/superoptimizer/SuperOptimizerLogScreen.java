package dev.kardipopo.superoptimizer;

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
        int gap = 6;
        int side = Math.max(70, (this.width - gap * 4) / 3);
        int y1 = this.height - 52;
        int y2 = this.height - 28;

        addRenderableWidget(Button.builder(Component.translatable("superoptimizer.logs.clear"),
            b -> SuperOptimizerLog.clear())
            .bounds(gap, y1, side, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("superoptimizer.logs.open_file"),
            b -> openLogFile())
            .bounds(gap + side + gap, y1, side, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("superoptimizer.logs.refresh"),
            b -> scroll = 0)
            .bounds(gap + (side + gap) * 2, y1, side, 20).build());

        int doneW = Math.min(180, Math.max(90, this.width - 16));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
            b -> close())
            .bounds(this.width / 2 - doneW / 2, y2, doneW, 20).build());
    }

    private void openLogFile() {
        Path path = minecraft.gameDirectory.toPath().resolve("config").resolve("superoptimizer.log");
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(path.toFile());
            } else {
                SuperOptimizerLog.info("Открытие внешнего файла недоступно на этой системе.");
            }
        } catch (Exception e) {
            SuperOptimizerLog.warn("Не удалось открыть файл логов: " + e.getMessage());
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY >= 36 && mouseY <= this.height - 58) {
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
        int bottom = this.height - 58;
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

        if (maxScroll > 0) {
            int trackTop = top;
            int trackBottom = bottom;
            int track = trackBottom - trackTop;
            int thumb = Math.max(18, (int) (track * track / (double) (track + maxScroll)));
            int thumbY = trackTop + (int) ((track - thumb) * (scroll / maxScroll));
            graphics.fill(this.width - 7, trackTop, this.width - 4, trackBottom, 0x551A1F26);
            graphics.fill(this.width - 7, thumbY, this.width - 4, thumbY + thumb, 0xFF4CB9FF);
        }
    }

    @Override
    public void onClose() {
        close();
    }

    private void close() {
        minecraft.setScreenAndShow(parent);
    }
}
