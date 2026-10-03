package dev.kardipopo.superoptimizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class SuperOptimizerScreen extends Screen {
    private enum Category {
        GENERAL,
        MICROWAVE,
        LIGHT,
        BALANCED,
        ADVANCED,
        COMPATIBILITY
    }

    private record Row(Button button, Component description, int baseY) {}

    private final Screen parent;
    private final SuperOptimizerConfig config;
    private final Category category;
    private final List<Row> rows = new ArrayList<>();
    private double scroll;
    private double maxScroll;

    public SuperOptimizerScreen(Screen parent, SuperOptimizerConfig config) {
        this(parent, config, Category.GENERAL);
    }

    public SuperOptimizerScreen(Screen parent, SuperOptimizerConfig config, Category category) {
        super(Component.translatable("superoptimizer.gui.title"));
        this.parent = parent;
        this.config = config;
        this.category = category;
    }

    @Override
    protected void init() {
        rows.clear();
        int cx = this.width / 2;

        int tabW = 100;
        int tabY = 20;
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            Category target = cats[i];
            int x = cx - (cats.length * tabW) / 2 + i * tabW;
            addRenderableWidget(Button.builder(categoryName(target), b -> minecraft.gui.setScreen(
                new SuperOptimizerScreen(parent, config, target)
            )).bounds(x, tabY, tabW - 3, 20).build());
        }

        addRenderableWidget(Button.builder(Component.translatable("superoptimizer.button.shaders"),
            b -> minecraft.gui.setScreen(new ShaderPackScreen(this, config)))
            .bounds(cx + 160, tabY, 100, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("superoptimizer.button.logs"),
            b -> minecraft.gui.setScreen(new SuperOptimizerLogScreen(this)))
            .bounds(cx - 260, tabY, 100, 20).build());

        int y = 70;
        switch (category) {
            case GENERAL -> {
                y = toggleRow("superoptimizer.option.enabled", "superoptimizer.desc.enabled",
                    config.enabled, v -> { config.enabled = v; SuperOptimizerClient.applyConfig(); }, y);
                y = toggleRow("superoptimizer.option.entity", "superoptimizer.desc.entity",
                    config.entityCulling, v -> config.entityCulling = v, y);
                y = toggleRow("superoptimizer.option.block_entity", "superoptimizer.desc.block_entity",
                    config.blockEntityCulling, v -> config.blockEntityCulling = v, y);
                y = toggleRow("superoptimizer.option.pause_motion", "superoptimizer.desc.pause_motion",
                    config.pauseDuringCameraMotion, v -> config.pauseDuringCameraMotion = v, y);
                y = cycleRow("superoptimizer.option.workers", "superoptimizer.desc.workers",
                    config.workerThreads, 1, 16, v -> config.workerThreads = v, y);
                y = cycleRow("superoptimizer.option.reserved", "superoptimizer.desc.reserved",
                    config.reservedCores, 0, Math.min(8, Math.max(0, Runtime.getRuntime().availableProcessors() - 1)),
                    v -> { config.reservedCores = v; SuperOptimizerClient.applyConfig(); }, y);
                addActionRow("superoptimizer.action.save", "superoptimizer.desc.save", b -> saveAndStay(), y); y += 48;
                addActionRow("superoptimizer.action.reset", "superoptimizer.desc.reset", b -> {
                    SuperOptimizerClient.applyPreset(SuperOptimizerClient.Preset.BALANCED);
                    minecraft.gui.setScreen(new SuperOptimizerScreen(parent, config, Category.GENERAL));
                }, y);
            }
            case MICROWAVE -> {
                addPresetRow("superoptimizer.preset.microwave", "superoptimizer.desc.preset.microwave",
                    SuperOptimizerClient.Preset.MICROWAVE, y);
            }
            case LIGHT -> {
                addPresetRow("superoptimizer.preset.light", "superoptimizer.desc.preset.light",
                    SuperOptimizerClient.Preset.LIGHT, y);
            }
            case BALANCED -> {
                addPresetRow("superoptimizer.preset.balanced", "superoptimizer.desc.preset.balanced",
                    SuperOptimizerClient.Preset.BALANCED, y);
            }
            case ADVANCED -> {
                addPresetRow("superoptimizer.preset.advanced", "superoptimizer.desc.preset.advanced",
                    SuperOptimizerClient.Preset.ADVANCED, y);
            }
            case COMPATIBILITY -> {
                y = toggleRow("superoptimizer.option.iris_lock", "superoptimizer.desc.iris_lock",
                    config.disableCullingWithIris, v -> config.disableCullingWithIris = v, y);
                y = toggleRow("superoptimizer.option.entity_culling_lock", "superoptimizer.desc.entity_culling_lock",
                    config.disableCullingWithEntityCullingMod, v -> config.disableCullingWithEntityCullingMod = v, y);
                y = toggleRow("superoptimizer.option.diagnostics", "superoptimizer.desc.diagnostics",
                    config.diagnostics, v -> config.diagnostics = v, y);
                y = toggleRow("superoptimizer.option.file_logging", "superoptimizer.desc.file_logging",
                    config.fileLogging, v -> { config.fileLogging = v; SuperOptimizerLog.info("Файловое логирование: " + v); }, y);
                y = toggleRow("superoptimizer.option.shader_scan_async", "superoptimizer.desc.shader_scan_async",
                    config.shaderScanAsync, v -> config.shaderScanAsync = v, y);
            }
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> close())
            .bounds(cx - 100, this.height - 28, 200, 20).build());

        maxScroll = Math.max(0, y - (this.height - 48));
        applyScroll();
    }

    private int toggleRow(String key, String descKey, boolean value,
                          java.util.function.Consumer<Boolean> setter, int y) {
        final boolean[] state = {value};
        Button b = Button.builder(label(key, state[0]), button -> {
            state[0] = !state[0];
            setter.accept(state[0]);
            button.setMessage(label(key, state[0]));
            if (key.equals("superoptimizer.option.enabled")) {
                SuperOptimizerLog.info("Главный переключатель оптимизатора: " + (state[0] ? "ВКЛ" : "ВЫКЛ"));
            }
        }).bounds(this.width / 2 - 155, y, 310, 20).build();
        rows.add(new Row(b, Component.translatable(descKey), y));
        addRenderableWidget(b);
        return y + 48;
    }

    private int cycleRow(String key, String descKey, int value, int min, int max,
                         java.util.function.IntConsumer setter, int y) {
        final int[] state = {value};
        Button b = Button.builder(Component.translatable(key, state[0]), button -> {
            state[0] = state[0] >= max ? min : state[0] + 1;
            setter.accept(state[0]);
            button.setMessage(Component.translatable(key, state[0]));
        }).bounds(this.width / 2 - 155, y, 310, 20).build();
        rows.add(new Row(b, Component.translatable(descKey), y));
        addRenderableWidget(b);
        return y + 48;
    }

    private void addActionRow(String key, String descKey, java.util.function.Consumer<Button> action, int y) {
        Button b = Button.builder(Component.translatable(key), action)
            .bounds(this.width / 2 - 155, y, 310, 20).build();
        rows.add(new Row(b, Component.translatable(descKey), y));
        addRenderableWidget(b);
    }

    private void addPresetRow(String key, String descKey, SuperOptimizerClient.Preset preset, int y) {
        addActionRow(key, descKey, b -> {
            SuperOptimizerClient.applyPreset(preset);
            minecraft.setScreen(new SuperOptimizerScreen(parent, config, category));
        }, y);
        addActionRow("superoptimizer.action.apply_return", "superoptimizer.desc.apply_return",
            b -> { SuperOptimizerClient.applyPreset(preset); close(); }, y + 48);
    }

    private Component categoryName(Category c) {
        return switch (c) {
            case GENERAL -> Component.translatable("superoptimizer.category.general");
            case MICROWAVE -> Component.translatable("superoptimizer.category.microwave");
            case LIGHT -> Component.translatable("superoptimizer.category.light");
            case BALANCED -> Component.translatable("superoptimizer.category.balanced");
            case ADVANCED -> Component.translatable("superoptimizer.category.advanced");
            case COMPATIBILITY -> Component.translatable("superoptimizer.category.compatibility");
        };
    }

    private static Component label(String key, boolean value) {
        return Component.translatable(key, value ? "ВКЛ" : "ВЫКЛ");
    }

    private void applyScroll() {
        int top = 58;
        int bottom = this.height - 38;
        for (Row row : rows) {
            int y = row.baseY() - (int) scroll;
            row.button().setY(y);
            boolean visible = y + row.button().getHeight() >= top && y <= bottom;
            row.button().visible = visible;
            row.button().active = visible;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY >= 58 && mouseY <= this.height - 38) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - verticalAmount * 26));
            applyScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.text(this.font, this.title, this.width / 2, 5, 0xFFFFFFFF, true);
        graphics.text(this.font, Component.translatable("superoptimizer.gui.backend",
            Minecraft.getInstance().options.preferredGraphicsBackend().toString()),
            8, 46, 0xFF9FA8B5, false);

        int top = 58;
        int bottom = this.height - 38;
        graphics.enableScissor(0, top, this.width, bottom);

        for (Row row : rows) {
            int y = row.baseY() - (int) scroll + 24;
            if (y > top && y < bottom - 10) {
                graphics.text(this.font, row.description(), this.width / 2 - 155, y, 0xFFB4BBC7, false);
            }
        }

        graphics.disableScissor();

        if (maxScroll > 0) {
            int trackTop = top;
            int trackBottom = bottom;
            int trackHeight = trackBottom - trackTop;
            int thumbHeight = Math.max(18, (int) (trackHeight * trackHeight / (trackHeight + maxScroll)));
            int thumbY = trackTop + (int) ((trackHeight - thumbHeight) * (scroll / maxScroll));
            graphics.fill(this.width - 8, trackTop, this.width - 4, trackBottom, 0x551A1F26);
            graphics.fill(this.width - 8, thumbY, this.width - 4, thumbY + thumbHeight, 0xFF4CB9FF);
        }
    }

    private void saveAndStay() {
        config.save(minecraft.gameDirectory.toPath().resolve("config"));
        SuperOptimizerClient.applyConfig();
        SuperOptimizerLog.info("Настройки сохранены.");
    }

    @Override
    public void onClose() {
        close();
    }

    private void close() {
        config.save(minecraft.gameDirectory.toPath().resolve("config"));
        SuperOptimizerClient.applyConfig();
        minecraft.gui.setScreen(parent);
    }
}
