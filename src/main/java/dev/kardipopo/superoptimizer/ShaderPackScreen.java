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

    public ShaderPackScreen(Screen parent, SuperOptimizerConfig config) {
        super(Component.translatable("superoptimizer.shaders.title"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        packButtons.clear();

        addRenderableWidget(Button.builder(Component.translatable("superoptimizer.shaders.iris"),
            b -> {
                if (!IrisBridge.openMainScreen(this)) {
                    SuperOptimizerLog.warn("Iris не найден или его API несовместим с текущим клиентом.");
                }
            }).bounds(8, 34, 145, 20).build());

        Button importButton = Button.builder(Component.translatable("superoptimizer.shaders.import"),
            b -> ShaderPackCatalog.importZip(minecraft))
            .bounds(158, 34, 145, 20).build();
        importButton.setTooltip(Tooltip.create(Component.translatable("superoptimizer.desc.shader_import")));
        importButton.setTooltipDelay(Duration.ofMillis(300));
        addRenderableWidget(importButton);

        Button folder = Button.builder(Component.translatable("superoptimizer.shaders.folder"),
            b -> ShaderPackCatalog.openFolder(minecraft))
            .bounds(308, 34, 120, 20).build();
        folder.setTooltip(Tooltip.create(Component.translatable("superoptimizer.desc.shader_folder")));
        folder.setTooltipDelay(Duration.ofMillis(300));
        addRenderableWidget(folder);

        Button refresh = Button.builder(Component.translatable("superoptimizer.shaders.refresh"),
            b -> ShaderPackCatalog.refresh(minecraft))
            .bounds(433, 34, 95, 20).build();
        refresh.setTooltip(Tooltip.create(Component.translatable("superoptimizer.desc.shader_refresh")));
        refresh.setTooltipDelay(Duration.ofMillis(300));
        addRenderableWidget(refresh);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
            b -> minecraft.setScreenAndShow(parent))
            .bounds(this.width - 108, 34, 100, 20).build());

        ShaderPackCatalog.refresh(minecraft);
        rebuildPackButtons();
    }

    private void rebuildPackButtons() {
        for (Button b : packButtons) removeWidget(b);
        packButtons.clear();

        List<Path> packs = ShaderPackCatalog.snapshot();

        int top = 70;
        int row = 24;
        int bottom = this.height - 12;
        maxScroll = Math.max(0, packs.size() * row - (bottom - top));

        for (int i = 0; i < packs.size(); i++) {
            Path pack = packs.get(i);
            int y = top + i * row - (int) scroll;

            Button b = Button.builder(Component.literal(pack.getFileName().toString()), button -> {
                SuperOptimizerLog.info("Выбран shaderpack: " + pack.getFileName());
                if (!IrisBridge.openMainScreen(this)) {
                    SuperOptimizerLog.warn("Для фактического применения shaderpack нужен совместимый shader loader.");
                }
            }).bounds(this.width / 2 - 220, y, 440, 20).build();

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
        if (mouseY >= 66 && mouseY <= this.height - 10 && maxScroll > 0) {
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
                if (minecraft.screen() == this) rebuildPackButtons();
            });
        }

        graphics.text(this.font, this.title, 8, 10, 0xFFFFFFFF, true);

        Component state = IrisBridge.isLoaded()
            ? Component.translatable("superoptimizer.shaders.iris_detected", IrisBridge.shadersInUse() ? "ВКЛ" : "ВЫКЛ")
            : Component.translatable("superoptimizer.shaders.iris_missing");
        graphics.text(this.font, state, 8, 56, 0xFFB4BBC7, false);

        if (maxScroll > 0) {
            int top = 66;
            int bottom = this.height - 10;
            int track = bottom - top;
            int thumb = Math.max(18, (int) (track * track / (double) (track + maxScroll)));
            int thumbY = top + (int) ((track - thumb) * (scroll / maxScroll));
            graphics.fill(this.width - 7, top, this.width - 4, bottom, 0x551A1F26);
            graphics.fill(this.width - 7, thumbY, this.width - 4, thumbY + thumb, 0xFF4CB9FF);
        }
    }
}
