package dev.kardipopo.superoptimizer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class ShaderPackScreen extends Screen {
    private final Screen parent;
    private final SuperOptimizerConfig config;
    private final List<Button> packButtons = new ArrayList<>();

    private double scroll;
    private double maxScroll;
    private int lastFingerprint = Integer.MIN_VALUE;
    private boolean refreshQueued;
    private boolean closed;

    public ShaderPackScreen(Screen parent, SuperOptimizerConfig config) {
        super(Component.translatable("superoptimizer.shaders.title"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        packButtons.clear();

        int gap = 5;
        int buttonCount = 5;
        int buttonW = Math.max(52, (this.width - gap * (buttonCount + 1)) / buttonCount);
        int x = gap;

        addTopButton(Component.translatable("superoptimizer.shaders.iris"),
            b -> {
                if (!IrisBridge.openMainScreen(this)) {
                    SuperOptimizerLog.warn("Не удалось открыть совместимый Iris screen.");
                }
            }, x, buttonW, "superoptimizer.desc.shader_iris");
        x += buttonW + gap;

        Button toggle = Button.builder(Component.translatable(
                "superoptimizer.shaders.toggle", IrisBridge.shadersEnabled() ? "ВКЛ" : "ВЫКЛ"),
            b -> {
                if (IrisBridge.setShadersEnabled(!IrisBridge.shadersEnabled())) {
                    b.setMessage(Component.translatable(
                        "superoptimizer.shaders.toggle", IrisBridge.shadersEnabled() ? "ВКЛ" : "ВЫКЛ"));
                }
            }).bounds(x, 34, buttonW, 20).build();
        toggle.setTooltip(Tooltip.create(Component.translatable("superoptimizer.desc.shader_toggle")));
        toggle.setTooltipDelay(Duration.ofMillis(300));
        toggle.active = IrisBridge.isLoaded();
        addRenderableWidget(toggle);
        x += buttonW + gap;

        addTopButton(Component.translatable("superoptimizer.shaders.import"),
            b -> ShaderPackCatalog.importZip(minecraft), x, buttonW, "superoptimizer.desc.shader_import");
        x += buttonW + gap;

        addTopButton(Component.translatable("superoptimizer.shaders.folder"),
            b -> ShaderPackCatalog.openFolder(minecraft), x, buttonW, "superoptimizer.desc.shader_folder");
        x += buttonW + gap;

        addTopButton(Component.translatable("superoptimizer.shaders.refresh"),
            b -> ShaderPackCatalog.refresh(minecraft), x, buttonW, "superoptimizer.desc.shader_refresh");

        int doneW = Math.min(180, Math.max(90, this.width - 16));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
            b -> close())
            .bounds(this.width / 2 - doneW / 2, this.height - 28, doneW, 20).build());

        ShaderPackCatalog.refresh(minecraft);
        rebuildPackButtons();
    }

    private void addTopButton(Component text, Button.OnPress press, int x, int width, String tooltipKey) {
        Button b = Button.builder(text, press).bounds(x, 34, width, 20).build();
        b.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
        b.setTooltipDelay(Duration.ofMillis(300));
        addRenderableWidget(b);
    }

    private void rebuildPackButtons() {
        for (Button b : packButtons) removeWidget(b);
        packButtons.clear();

        List<Path> packs = ShaderPackCatalog.snapshot();

        int left = 8;
        int right = this.width - 8;
        int top = 72;
        int bottom = this.height - 38;
        int rowHeight = 26;
        int rowWidth = Math.min(560, Math.max(150, right - left));

        maxScroll = Math.max(0, packs.size() * rowHeight - (bottom - top));
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        for (int i = 0; i < packs.size(); i++) {
            Path pack = packs.get(i);
            int y = top + i * rowHeight - (int) scroll;

            Button b = Button.builder(Component.literal(pack.getFileName().toString()), button -> {
                SuperOptimizerLog.info("Shaderpack выбран: " + pack.getFileName());
                if (!IrisBridge.openMainScreen(this)) {
                    SuperOptimizerLog.warn("Для фактического применения shaderpack нужен совместимый shader loader.");
                }
            }).bounds(this.width / 2 - rowWidth / 2, y, rowWidth, 20).build();

            b.setTooltip(Tooltip.create(Component.translatable("superoptimizer.desc.shader_pack_row")));
            b.setTooltipDelay(Duration.ofMillis(300));

            boolean visible = y + 20 >= top && y <= bottom;
            b.setVisible(visible);
            b.active = visible;

            packButtons.add(b);
            addRenderableWidget(b);
        }

        lastFingerprint = ShaderPackCatalog.fingerprint();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY >= 68 && mouseY <= this.height - 36 && maxScroll > 0) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - verticalAmount * 24));
            rebuildPackButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int fingerprint = ShaderPackCatalog.fingerprint();
        if (fingerprint != lastFingerprint && !refreshQueued) {
            refreshQueued = true;
            minecraft.execute(() -> {
                refreshQueued = false;
                if (!closed) rebuildPackButtons();
            });
        }

        graphics.text(this.font, this.title, 8, 10, 0xFFFFFFFF, true);

        Component state = IrisBridge.isLoaded()
            ? Component.translatable("superoptimizer.shaders.iris_detected",
                IrisBridge.shadersInUse() ? "ВКЛ" : "ВЫКЛ")
            : Component.translatable("superoptimizer.shaders.iris_missing");
        graphics.text(this.font, state, 8, 56, 0xFFB4BBC7, false);

        if (maxScroll > 0) {
            int top = 68;
            int bottom = this.height - 36;
            int track = bottom - top;
            int thumb = Math.max(18, (int) (track * track / (double) (track + maxScroll)));
            int thumbY = top + (int) ((track - thumb) * (scroll / maxScroll));
            graphics.fill(this.width - 7, top, this.width - 4, bottom, 0x551A1F26);
            graphics.fill(this.width - 7, thumbY, this.width - 4, thumbY + thumb, 0xFF4CB9FF);
        }
    }

    @Override
    public void onClose() {
        close();
    }

    private void close() {
        closed = true;
        minecraft.setScreenAndShow(parent);
    }
}
