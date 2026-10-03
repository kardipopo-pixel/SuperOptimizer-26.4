package dev.kardipopo.superoptimizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public final class SuperOptimizerScreen extends Screen {
    private enum Category {
        GENERAL("superoptimizer.category.general"),
        PROFILES("superoptimizer.category.profiles"),
        CULLING("superoptimizer.category.culling"),
        CPU("superoptimizer.category.cpu"),
        COMPATIBILITY("superoptimizer.category.compatibility"),
        DIAGNOSTICS("superoptimizer.category.diagnostics");

        final String key;

        Category(String key) {
            this.key = key;
        }
    }

    private record Entry(Button button, int baseY) {}

    private final Screen parent;
    private final SuperOptimizerConfig config;
    private final Category category;

    private final List<Button> pageWidgets = new ArrayList<>();
    private final List<Entry> scrollEntries = new ArrayList<>();

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
        clearLists();

        int sidebarX = 8;
        int sidebarW = Math.max(105, Math.min(138, this.width / 4));
        int contentLeft = sidebarX + sidebarW + 8;
        int contentRight = this.width - 8;
        int contentW = Math.max(180, contentRight - contentLeft);
        int contentCenter = contentLeft + contentW / 2;

        addSidebarButtons(sidebarX, sidebarW);
        addRenderableWidget(Button.builder(Component.translatable("superoptimizer.button.shaders"),
            b -> minecraft.setScreenAndShow(new ShaderPackScreen(this, config)))
            .bounds(contentLeft, this.height - 54, Math.min(190, contentW), 20)
            .build());

        addRenderableWidget(Button.builder(Component.translatable("superoptimizer.button.logs"),
            b -> minecraft.setScreenAndShow(new SuperOptimizerLogScreen(this)))
            .bounds(contentLeft, this.height - 30, Math.min(190, contentW), 20)
            .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> close())
            .bounds(Math.max(contentRight - 160, contentLeft), this.height - 30, 160, 20)
            .build());

        int y = 48;
        switch (category) {
            case GENERAL -> y = buildGeneral(contentCenter, contentW, y);
            case PROFILES -> y = buildProfiles(contentCenter, contentW, y);
            case CULLING -> y = buildCulling(contentCenter, contentW, y);
            case CPU -> y = buildCpu(contentCenter, contentW, y);
            case COMPATIBILITY -> y = buildCompatibility(contentCenter, contentW, y);
            case DIAGNOSTICS -> y = buildDiagnostics(contentCenter, contentW, y);
        }

        maxScroll = Math.max(0, y - (this.height - 62));
        applyScroll();
    }

    private void clearLists() {
        pageWidgets.clear();
        scrollEntries.clear();
        scroll = 0;
        maxScroll = 0;
    }

    private void addSidebarButtons(int x, int width) {
        int y = 34;
        for (Category c : Category.values()) {
            Button b = Button.builder(Component.translatable(c.key), button ->
                minecraft.setScreenAndShow(new SuperOptimizerScreen(parent, config, c)))
                .bounds(x, y, width, 20)
                .build();
            addRenderableWidget(b);
            pageWidgets.add(b);
            y += 23;
        }
    }

    private int buildGeneral(int center, int width, int y) {
        y = toggle("superoptimizer.option.enabled", "superoptimizer.desc.enabled", config.enabled,
            v -> { config.enabled = v; SuperOptimizerClient.applyConfig(); }, center, width, y);

        y = toggle("superoptimizer.option.background", "superoptimizer.desc.background",
            config.backgroundTasks, v -> { config.backgroundTasks = v; SuperOptimizerClient.applyConfig(); }, center, width, y);

        y = toggle("superoptimizer.option.entity", "superoptimizer.desc.entity",
            config.entityCulling, v -> config.entityCulling = v, center, width, y);

        y = toggle("superoptimizer.option.block_entity", "superoptimizer.desc.block_entity",
            config.blockEntityCulling, v -> config.blockEntityCulling = v, center, width, y);

        y = toggle("superoptimizer.option.pause_motion", "superoptimizer.desc.pause_motion",
            config.pauseDuringCameraMotion, v -> config.pauseDuringCameraMotion = v, center, width, y);

        y = toggle("superoptimizer.option.skip_near", "superoptimizer.desc.skip_near",
            config.skipNearEntityCulling, v -> config.skipNearEntityCulling = v, center, width, y);

        y = cycle("superoptimizer.option.near_distance", "superoptimizer.desc.near_distance",
            config.nearEntityDistance, 0, 64, 4, v -> config.nearEntityDistance = v, center, width, y);

        y = toggle("superoptimizer.option.directional", "superoptimizer.desc.directional",
            config.directionalEntityCulling, v -> config.directionalEntityCulling = v, center, width, y);

        return y;
    }

    private int buildProfiles(int center, int width, int y) {
        y = preset("superoptimizer.preset.microwave", "superoptimizer.desc.preset.microwave",
            SuperOptimizerClient.Preset.MICROWAVE, center, width, y);
        y = preset("superoptimizer.preset.light", "superoptimizer.desc.preset.light",
            SuperOptimizerClient.Preset.LIGHT, center, width, y);
        y = preset("superoptimizer.preset.balanced", "superoptimizer.desc.preset.balanced",
            SuperOptimizerClient.Preset.BALANCED, center, width, y);
        y = preset("superoptimizer.preset.advanced", "superoptimizer.desc.preset.advanced",
            SuperOptimizerClient.Preset.ADVANCED, center, width, y);

        addStatus("superoptimizer.profile.note", center, y);
        return y + 28;
    }

    private int buildCulling(int center, int width, int y) {
        y = toggle("superoptimizer.option.entity", "superoptimizer.desc.entity",
            config.entityCulling, v -> config.entityCulling = v, center, width, y);
        y = toggle("superoptimizer.option.block_entity", "superoptimizer.desc.block_entity",
            config.blockEntityCulling, v -> config.blockEntityCulling = v, center, width, y);
        y = toggle("superoptimizer.option.pause_motion", "superoptimizer.desc.pause_motion",
            config.pauseDuringCameraMotion, v -> config.pauseDuringCameraMotion = v, center, width, y);
        y = toggle("superoptimizer.option.skip_near", "superoptimizer.desc.skip_near",
            config.skipNearEntityCulling, v -> config.skipNearEntityCulling = v, center, width, y);
        y = cycle("superoptimizer.option.near_distance", "superoptimizer.desc.near_distance",
            config.nearEntityDistance, 0, 64, 4, v -> config.nearEntityDistance = v, center, width, y);
        y = toggle("superoptimizer.option.directional", "superoptimizer.desc.directional",
            config.directionalEntityCulling, v -> config.directionalEntityCulling = v, center, width, y);
        return y;
    }

    private int buildCpu(int center, int width, int y) {
        y = toggle("superoptimizer.option.background", "superoptimizer.desc.background",
            config.backgroundTasks, v -> { config.backgroundTasks = v; SuperOptimizerClient.applyConfig(); }, center, width, y);
        y = cycle("superoptimizer.option.workers", "superoptimizer.desc.workers",
            config.workerThreads, 1, Math.max(1, Math.min(16, Runtime.getRuntime().availableProcessors())), 1,
            v -> { config.workerThreads = v; SuperOptimizerClient.applyConfig(); }, center, width, y);
        y = cycle("superoptimizer.option.reserved", "superoptimizer.desc.reserved",
            config.reservedCores, 0, Math.min(8, Math.max(0, Runtime.getRuntime().availableProcessors() - 1)), 1,
            v -> { config.reservedCores = v; SuperOptimizerClient.applyConfig(); }, center, width, y);
        y = toggle("superoptimizer.option.shader_scan_async", "superoptimizer.desc.shader_scan_async",
            config.shaderScanAsync, v -> { config.shaderScanAsync = v; SuperOptimizerClient.applyConfig(); }, center, width, y);
        return y;
    }

    private int buildCompatibility(int center, int width, int y) {
        y = toggle("superoptimizer.option.iris_lock", "superoptimizer.desc.iris_lock",
            config.disableCullingWithIris, v -> config.disableCullingWithIris = v, center, width, y);
        y = toggle("superoptimizer.option.entity_culling_lock", "superoptimizer.desc.entity_culling_lock",
            config.disableCullingWithEntityCullingMod, v -> config.disableCullingWithEntityCullingMod = v, center, width, y);
        y = addStatus("superoptimizer.compat.iris", center, y);
        y = addStatus("superoptimizer.compat.entity_culling", center, y);
        return y + 8;
    }

    private int buildDiagnostics(int center, int width, int y) {
        y = toggle("superoptimizer.option.diagnostics", "superoptimizer.desc.diagnostics",
            config.diagnostics, v -> config.diagnostics = v, center, width, y);
        y = toggle("superoptimizer.option.file_logging", "superoptimizer.desc.file_logging",
            config.fileLogging, v -> { config.fileLogging = v; SuperOptimizerLog.info("Файловое логирование: " + v); }, center, width, y);

        y = addStatus("superoptimizer.diag.entity", center, y);
        y = addStatus("superoptimizer.diag.block_entity", center, y);

        addAction("superoptimizer.action.clear_logs", "superoptimizer.desc.clear_logs",
            b -> SuperOptimizerLog.clear(), center, width, y); y += 42;

        addAction("superoptimizer.action.reset_stats", "superoptimizer.desc.reset_stats",
            b -> CullingContext.resetStats(), center, width, y); y += 42;

        return y;
    }

    private int toggle(String key, String descKey, boolean value, Consumer<Boolean> setter,
                       int center, int width, int y) {
        final boolean[] state = {value};
        Button b = Button.builder(label(key, state[0]), button -> {
            state[0] = !state[0];
            setter.accept(state[0]);
            button.setMessage(label(key, state[0]));
        }).bounds(center - Math.min(210, width / 2), y, Math.min(420, width), 20).build();

        tooltip(b, descKey);
        addEntry(b, y);
        return y + 43;
    }

    private int cycle(String key, String descKey, int value, int min, int max, int step,
                      IntConsumer setter, int center, int width, int y) {
        final int[] state = {value};
        Button b = Button.builder(Component.translatable(key, state[0]), button -> {
            state[0] += step;
            if (state[0] > max) state[0] = min;
            setter.accept(state[0]);
            button.setMessage(Component.translatable(key, state[0]));
        }).bounds(center - Math.min(210, width / 2), y, Math.min(420, width), 20).build();

        tooltip(b, descKey);
        addEntry(b, y);
        return y + 43;
    }

    private int preset(String key, String descKey, SuperOptimizerClient.Preset preset,
                       int center, int width, int y) {
        Button b = Button.builder(Component.translatable(key), button -> {
            SuperOptimizerClient.applyPreset(preset);
            minecraft.setScreenAndShow(new SuperOptimizerScreen(parent, config, Category.PROFILES));
        }).bounds(center - Math.min(210, width / 2), y, Math.min(420, width), 20).build();

        tooltip(b, descKey);
        addEntry(b, y);
        return y + 43;
    }

    private void addAction(String key, String descKey, Consumer<Button> action, int center, int width, int y) {
        Button b = Button.builder(Component.translatable(key), button -> action.accept(button))
            .bounds(center - Math.min(210, width / 2), y, Math.min(420, width), 20)
            .build();
        tooltip(b, descKey);
        addEntry(b, y);
    }

    private int addStatus(String key, int center, int y) {
        Button b = Button.builder(Component.translatable(key, statusValue(key)), x -> {})
            .bounds(center - 210, y, 420, 20)
            .build();
        b.active = false;
        addEntry(b, y);
        return y + 28;
    }

    private String statusValue(String key) {
        if (key.equals("superoptimizer.compat.iris")) {
            return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris") ? "обнаружен" : "не установлен";
        }
        if (key.equals("superoptimizer.compat.entity_culling")) {
            return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("entityculling") ? "обнаружен" : "не установлен";
        }
        if (key.equals("superoptimizer.diag.entity")) {
            return CullingContext.entityCulled() + " / " + CullingContext.entityChecks();
        }
        if (key.equals("superoptimizer.diag.block_entity")) {
            return CullingContext.blockEntityCulled() + " / " + CullingContext.blockEntityChecks();
        }
        return "";
    }

    private void addEntry(Button b, int baseY) {
        scrollEntries.add(new Entry(b, baseY));
        addRenderableWidget(b);
    }

    private void tooltip(Button b, String key) {
        b.setTooltip(Tooltip.create(Component.translatable(key)));
        b.setTooltipDelay(Duration.ofMillis(300));
    }

    private static Component label(String key, boolean value) {
        return Component.translatable(key, value ? "ВКЛ" : "ВЫКЛ");
    }

    private void applyScroll() {
        int top = 46;
        int bottom = this.height - 60;

        for (Entry entry : scrollEntries) {
            int y = entry.baseY() - (int) scroll;
            entry.button().setY(y);
            boolean visible = y + entry.button().getHeight() >= top && y <= bottom;
            entry.button().setVisible(visible);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int left = Math.max(0, 8 + 138);
        if (mouseX >= left && mouseY >= 42 && mouseY <= this.height - 58 && maxScroll > 0) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - verticalAmount * 30));
            applyScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.text(this.font, this.title, 8, 8, 0xFFFFFFFF, true);
        graphics.text(this.font, Component.translatable("superoptimizer.gui.backend",
            Minecraft.getInstance().options.preferredGraphicsBackend().toString()),
            8, 22, 0xFF9FA8B5, false);

        if (maxScroll > 0) {
            int top = 46;
            int bottom = this.height - 60;
            int trackHeight = bottom - top;
            int thumbHeight = Math.max(20, (int) (trackHeight * trackHeight / (trackHeight + maxScroll)));
            int thumbY = top + (int) ((trackHeight - thumbHeight) * (scroll / maxScroll));
            graphics.fill(this.width - 7, top, this.width - 4, bottom, 0x551A1F26);
            graphics.fill(this.width - 7, thumbY, this.width - 4, thumbY + thumbHeight, 0xFF4CB9FF);
        }
    }

    @Override
    public void onClose() {
        close();
    }

    private void close() {
        config.save(minecraft.gameDirectory.toPath().resolve("config"));
        SuperOptimizerClient.applyConfig();
        minecraft.setScreenAndShow(parent);
    }
}
