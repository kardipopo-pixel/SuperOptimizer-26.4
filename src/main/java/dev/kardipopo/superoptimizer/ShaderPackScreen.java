package dev.kardipopo.superoptimizer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ShaderPackScreen extends Screen {
    private final Screen parent;
    private final SuperOptimizerConfig config;
    private double scroll;
    private double maxScroll;
    private final List<Button> fileButtons = new ArrayList<>();

    public ShaderPackScreen(Screen parent, SuperOptimizerConfig config) {
        super(Component.translatable("superoptimizer.shaders.title"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        fileButtons.clear();

        addRenderableWidget(Button.builder(Component.translatable("superoptimizer.shaders.import"),
            b -> { ShaderPackCatalog.importZip(minecraft); SuperOptimizerLog.info("Открыт импорт shaderpack."); })
            .bounds(8, 34, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("superoptimizer.shaders.folder"),
            b -> ShaderPackCatalog.openFolder(minecraft)).bounds(164, 34, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("superoptimizer.shaders.refresh"),
            b -> ShaderPackCatalog.refresh(minecraft)).bounds(320, 34, 110, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
            b -> minecraft.gui.setScreen(parent)).bounds(this.width - 108, 34, 100, 20).build());

        ShaderPackCatalog.refresh(minecraft);
        layoutFiles();
    }

    private void layoutFiles() {
        List<Path> packs = ShaderPackCatalog.snapshot();
        int top = 66;
        int rowHeight = 26;
        int visibleBottom = height - 8;
        maxScroll = Math.max(0, packs.size() * rowHeight - (visibleBottom - top));
        for (int i = 0; i < fileButtons.size(); i++) removeWidget(fileButtons.get(i));
        fileButtons.clear();

        for (int i = 0; i < packs.size(); i++) {
            Path path = packs.get(i);
            int y = top + i * rowHeight - (int) scroll;
            Button b = Button.builder(Component.literal(path.getFileName().toString()), button -> {
                SuperOptimizerLog.info("Shaderpack выбран для просмотра: " + path.getFileName());
            }).bounds(this.width / 2 - 220, y, 440, 20).build();
            b.visible = y + 20 >= top && y <= visibleBottom;
            b.active = b.visible;
            fileButtons.add(b);
            addRenderableWidget(b);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY >= 62) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - verticalAmount * 26));
            layoutFiles();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(this.font, this.title, 8, 10, 0xFFFFFFFF, true);

        boolean iris = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris");
        graphics.text(this.font, Component.translatable(
            iris ? "superoptimizer.shaders.iris_detected" : "superoptimizer.shaders.iris_missing"),
            8, 56, 0xFFB4BBC7, false);

        graphics.enableScissor(0, 64, this.width, this.height - 4);
        graphics.disableScissor();
    }
}
