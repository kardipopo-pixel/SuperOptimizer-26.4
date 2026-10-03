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
import java.util.function.Supplier;

/**
 * Graphics settings screen inspired by Sodium's compact list layout:
 * fixed left navigation, narrow centered settings column, no screen overflow.
 */
public final class SuperOptimizerScreen extends Screen {
    private enum Category {
        GENERAL("superoptimizer.category.general"),
        QUALITY("superoptimizer.category.quality"),
        PERFORMANCE("superoptimizer.category.performance"),
        OPTIMIZATIONS("superoptimizer.category.optimizations"),
        CULLING("superoptimizer.category.culling"),
        CPU("superoptimizer.category.cpu"),
        COMPATIBILITY("superoptimizer.category.compatibility"),
        SHADERS("superoptimizer.shaders.title"),
        DIAGNOSTICS("superoptimizer.category.diagnostics");

        final String key;
        Category(String key) { this.key = key; }
    }

    private enum Kind { TOGGLE, VALUE, ACTION, STATUS, PROFILE }

    private record Row(Button hitbox, String key, String desc, Supplier<Component> value, int y, Kind kind) {}
    private record NavButton(Category category, Button hitbox, int y) {}

    private final Screen parent;
    private final SuperOptimizerConfig config;
    private final Category category;

    private final List<Row> rows = new ArrayList<>();
    private final List<NavButton> nav = new ArrayList<>();

    private int navX, navW, mainX, mainW;
    private int contentTop, contentBottom, pageBottom;
    private double scroll, maxScroll;
    private SuperOptimizerClient.Preset activePreset;

    public SuperOptimizerScreen(Screen parent, SuperOptimizerConfig config) {
        this(parent, config, Category.GENERAL);
    }

    public SuperOptimizerScreen(Screen parent, SuperOptimizerConfig config, Category category) {
        super(Component.translatable("superoptimizer.gui.graphics_title"));
        this.parent = parent;
        this.config = config;
        this.category = category;
        this.activePreset = detectPreset();
    }

    @Override
    protected void init() {
        rows.clear();
        nav.clear();
        scroll = 0;

        int margin = 16;
        int gap = 14;

        navX = margin;
        navW = Math.min(250, Math.max(190, this.width / 4));

        mainX = navX + navW + gap;
        mainW = Math.min(560, this.width - mainX - margin);

        if (mainW < 330) {
            navW = Math.max(170, this.width / 3);
            mainX = navX + navW + gap;
            mainW = this.width - mainX - margin;
        }

        mainW = Math.max(260, mainW);

        contentTop = 92;
        contentBottom = this.height - 48;

        buildNavigation();
        buildPage();

        maxScroll = Math.max(0, pageBottom - contentBottom + 8);
        applyScroll();
    }

    private void buildNavigation() {
        int y = 54;

        // Mod header, similar to the left column in the reference.
        for (Category c : Category.values()) {
            Button b = hitbox(Component.translatable(c.key),
                    q -> minecraft.setScreenAndShow(new SuperOptimizerScreen(parent, config, c)),
                    navX, y, navW, 34);
            nav.add(new NavButton(c, b, y));
            y += 36;
        }
    }

    private void buildPage() {
        int y = contentTop + 38;

        switch (category) {
            case GENERAL -> {
                y = header(y, "superoptimizer.category.general", "superoptimizer.desc.enabled");
                y = profile(y);
                y = toggle(y, "superoptimizer.option.enabled", "superoptimizer.desc.enabled",
                        () -> config.enabled, v -> {
                            config.enabled = v;
                            SuperOptimizerClient.applyConfig();
                        });
                y = toggle(y, "superoptimizer.option.background", "superoptimizer.desc.background",
                        () -> config.backgroundTasks, v -> {
                            config.backgroundTasks = v;
                            SuperOptimizerClient.applyConfig();
                        });
            }
            case QUALITY -> {
                y = header(y, "superoptimizer.category.quality", "superoptimizer.desc.pause_motion");
                y = toggle(y, "superoptimizer.option.pause_motion", "superoptimizer.desc.pause_motion",
                        () -> config.pauseDuringCameraMotion, v -> config.pauseDuringCameraMotion = v);
                y = toggle(y, "superoptimizer.option.skip_near", "superoptimizer.desc.skip_near",
                        () -> config.skipNearEntityCulling, v -> config.skipNearEntityCulling = v);
                y = cycle(y, "superoptimizer.option.near_distance", "superoptimizer.desc.near_distance",
                        () -> config.nearEntityDistance, 0, 64, 4, v -> config.nearEntityDistance = v);
            }
            case PERFORMANCE -> {
                y = header(y, "superoptimizer.category.performance", "superoptimizer.desc.directional");
                y = toggle(y, "superoptimizer.option.entity", "superoptimizer.desc.entity",
                        () -> config.entityCulling, v -> config.entityCulling = v);
                y = toggle(y, "superoptimizer.option.block_entity", "superoptimizer.desc.block_entity",
                        () -> config.blockEntityCulling, v -> config.blockEntityCulling = v);
                y = toggle(y, "superoptimizer.option.directional", "superoptimizer.desc.directional",
                        () -> config.directionalEntityCulling, v -> config.directionalEntityCulling = v);
            }
            case OPTIMIZATIONS -> {
                y = header(y, "superoptimizer.category.optimizations", "superoptimizer.desc.background");
                y = status(y, "superoptimizer.optimizations.culling",
                        () -> Component.literal(config.entityCulling ? "активен" : "отключен"));
                y = status(y, "superoptimizer.optimizations.background",
                        () -> Component.literal(config.backgroundTasks ? "активны" : "отключены"));
                y = status(y, "superoptimizer.optimizations.workers",
                        () -> Component.literal(Integer.toString(config.workerThreads)));
                y = status(y, "superoptimizer.optimizations.reserved",
                        () -> Component.literal(Integer.toString(config.reservedCores)));
            }
            case CULLING -> {
                y = header(y, "superoptimizer.category.culling", "superoptimizer.desc.entity");
                y = toggle(y, "superoptimizer.option.entity", "superoptimizer.desc.entity",
                        () -> config.entityCulling, v -> config.entityCulling = v);
                y = toggle(y, "superoptimizer.option.block_entity", "superoptimizer.desc.block_entity",
                        () -> config.blockEntityCulling, v -> config.blockEntityCulling = v);
                y = toggle(y, "superoptimizer.option.directional", "superoptimizer.desc.directional",
                        () -> config.directionalEntityCulling, v -> config.directionalEntityCulling = v);
                y = toggle(y, "superoptimizer.option.skip_near", "superoptimizer.desc.skip_near",
                        () -> config.skipNearEntityCulling, v -> config.skipNearEntityCulling = v);
                y = cycle(y, "superoptimizer.option.near_distance", "superoptimizer.desc.near_distance",
                        () -> config.nearEntityDistance, 0, 64, 4, v -> config.nearEntityDistance = v);
            }
            case CPU -> {
                y = header(y, "superoptimizer.category.cpu", "superoptimizer.desc.workers");
                y = toggle(y, "superoptimizer.option.background", "superoptimizer.desc.background",
                        () -> config.backgroundTasks, v -> {
                            config.backgroundTasks = v;
                            SuperOptimizerClient.applyConfig();
                        });
                y = cycle(y, "superoptimizer.option.workers", "superoptimizer.desc.workers",
                        () -> config.workerThreads, 1,
                        Math.max(1, Math.min(16, Runtime.getRuntime().availableProcessors())),
                        1, v -> {
                            config.workerThreads = v;
                            SuperOptimizerClient.applyConfig();
                        });
                y = cycle(y, "superoptimizer.option.reserved", "superoptimizer.desc.reserved",
                        () -> config.reservedCores, 0,
                        Math.min(8, Math.max(0, Runtime.getRuntime().availableProcessors() - 1)),
                        1, v -> {
                            config.reservedCores = v;
                            SuperOptimizerClient.applyConfig();
                        });
                y = toggle(y, "superoptimizer.option.shader_scan_async", "superoptimizer.desc.shader_scan_async",
                        () -> config.shaderScanAsync, v -> {
                            config.shaderScanAsync = v;
                            SuperOptimizerClient.applyConfig();
                        });
            }
            case COMPATIBILITY -> {
                y = header(y, "superoptimizer.category.compatibility", "superoptimizer.desc.iris_lock");
                y = toggle(y, "superoptimizer.option.iris_lock", "superoptimizer.desc.iris_lock",
                        () -> config.disableCullingWithIris, v -> config.disableCullingWithIris = v);
                y = toggle(y, "superoptimizer.option.entity_culling_lock", "superoptimizer.desc.entity_culling_lock",
                        () -> config.disableCullingWithEntityCullingMod, v -> config.disableCullingWithEntityCullingMod = v);
                y = status(y, "superoptimizer.compat.iris", () -> Component.literal(
                        net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris")
                                ? "обнаружен" : "не установлен"));
                y = status(y, "superoptimizer.compat.entity_culling", () -> Component.literal(
                        net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("entityculling")
                                ? "обнаружен" : "не установлен"));
            }
            case SHADERS -> {
                y = header(y, "superoptimizer.shaders.title", "superoptimizer.desc.shader_iris");
                y = toggle(y, "superoptimizer.option.shader_scan_async", "superoptimizer.desc.shader_scan_async",
                        () -> config.shaderScanAsync, v -> {
                            config.shaderScanAsync = v;
                            SuperOptimizerClient.applyConfig();
                        });
                y = status(y, "superoptimizer.shaders.iris_detected", () -> Component.literal(
                        net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris")
                                ? "Iris обнаружен" : "Iris не найден"));
                y = action(y, "superoptimizer.button.shaders", "superoptimizer.desc.shader_iris",
                        q -> minecraft.setScreenAndShow(new ShaderPackScreen(this, config)));
            }
            case DIAGNOSTICS -> {
                y = header(y, "superoptimizer.category.diagnostics", "superoptimizer.desc.diagnostics");
                y = toggle(y, "superoptimizer.option.diagnostics", "superoptimizer.desc.diagnostics",
                        () -> config.diagnostics, v -> config.diagnostics = v);
                y = toggle(y, "superoptimizer.option.file_logging", "superoptimizer.desc.file_logging",
                        () -> config.fileLogging, v -> config.fileLogging = v);
                y = status(y, "superoptimizer.diag.entity",
                        () -> Component.literal(CullingContext.entityCulled() + " / " + CullingContext.entityChecks()));
                y = status(y, "superoptimizer.diag.block_entity",
                        () -> Component.literal(CullingContext.blockEntityCulled() + " / " + CullingContext.blockEntityChecks()));
                y = action(y, "superoptimizer.action.clear_logs", "superoptimizer.desc.clear_logs",
                        q -> SuperOptimizerLog.clear());
                y = action(y, "superoptimizer.action.reset_stats", "superoptimizer.desc.reset_stats",
                        q -> CullingContext.resetStats());
            }
        }

        pageBottom = y + 12;
    }

    private int header(int y, String title, String subtitle) {
        return y + 2;
    }

    private int profile(int y) {
        int h = 40;
        Button b = hitbox(Component.literal("Профиль"),
                q -> cycleProfile(),
                mainX, y, mainW, h);
        b.setTooltip(Tooltip.create(Component.literal("Переключить профиль, как в Iris")));
        b.setTooltipDelay(Duration.ofMillis(250));
        rows.add(new Row(b, "superoptimizer.profile.current", "superoptimizer.desc.profile.current",
                () -> profileName(activePreset), y, Kind.PROFILE));
        return y + h + 4;
    }

    private void cycleProfile() {
        SuperOptimizerClient.Preset next;
        if (activePreset == null) {
            next = SuperOptimizerClient.Preset.LIGHT;
        } else {
            next = switch (activePreset) {
                case LIGHT -> SuperOptimizerClient.Preset.BALANCED;
                case BALANCED -> SuperOptimizerClient.Preset.ADVANCED;
                case ADVANCED -> SuperOptimizerClient.Preset.MICROWAVE;
                case MICROWAVE -> SuperOptimizerClient.Preset.LIGHT;
            };
        }
        activePreset = next;
        SuperOptimizerClient.applyPreset(next);
        init();
    }

    private int toggle(int y, String key, String desc, Supplier<Boolean> get, Consumer<Boolean> set) {
        Button b = hitbox(Component.translatable(key), q -> {
            set.accept(!get.get());
            activePreset = detectPreset();
        }, mainX, y, mainW, 36);
        tooltip(b, desc);
        rows.add(new Row(b, key, desc, () -> Component.literal(get.get() ? "ВКЛ" : "ВЫКЛ"), y, Kind.TOGGLE));
        return y + 38;
    }

    private int cycle(int y, String key, String desc, Supplier<Integer> get, int min, int max, int step,
                      IntConsumer set) {
        Button b = hitbox(Component.translatable(key), q -> {
            int v = get.get() + step;
            if (v > max) v = min;
            set.accept(v);
            activePreset = detectPreset();
        }, mainX, y, mainW, 36);
        tooltip(b, desc);
        rows.add(new Row(b, key, desc, () -> Component.literal(Integer.toString(get.get())), y, Kind.VALUE));
        return y + 38;
    }

    private int status(int y, String key, Supplier<Component> value) {
        Button b = hitbox(Component.translatable(key), q -> {}, mainX, y, mainW, 36);
        b.active = false;
        rows.add(new Row(b, key, key, value, y, Kind.STATUS));
        return y + 38;
    }

    private int action(int y, String key, String desc, Consumer<Button> action) {
        Button b = hitbox(Component.translatable(key), action, mainX, y, mainW, 36);
        tooltip(b, desc);
        rows.add(new Row(b, key, desc, () -> Component.literal("Открыть"), y, Kind.ACTION));
        return y + 38;
    }

    private Button hitbox(Component label, Consumer<Button> action, int x, int y, int w, int h) {
        Button b = Button.builder(label, q -> action.accept(q))
                .bounds(x, y, Math.max(1, w), h)
                .build();
        b.setAlpha(0f);
        addRenderableWidget(b);
        return b;
    }

    private void tooltip(Button b, String key) {
        b.setTooltip(Tooltip.create(Component.translatable(key)));
        b.setTooltipDelay(Duration.ofMillis(250));
    }

    private Component profileName(SuperOptimizerClient.Preset preset) {
        if (preset == null) return Component.literal("Пользовательский");
        return switch (preset) {
            case LIGHT -> Component.translatable("superoptimizer.preset.light");
            case BALANCED -> Component.translatable("superoptimizer.preset.balanced");
            case ADVANCED -> Component.literal("Максимум FPS");
            case MICROWAVE -> Component.literal("Микроволновка");
        };
    }

    private SuperOptimizerClient.Preset detectPreset() {
        if (match(SuperOptimizerClient.Preset.MICROWAVE)) return SuperOptimizerClient.Preset.MICROWAVE;
        if (match(SuperOptimizerClient.Preset.LIGHT)) return SuperOptimizerClient.Preset.LIGHT;
        if (match(SuperOptimizerClient.Preset.BALANCED)) return SuperOptimizerClient.Preset.BALANCED;
        if (match(SuperOptimizerClient.Preset.ADVANCED)) return SuperOptimizerClient.Preset.ADVANCED;
        return null;
    }

    private boolean match(SuperOptimizerClient.Preset p) {
        return switch (p) {
            case MICROWAVE -> config.entityCulling && config.blockEntityCulling
                    && config.directionalEntityCulling && !config.backgroundTasks
                    && !config.shaderScanAsync && !config.diagnostics
                    && config.reservedCores == 1 && config.workerThreads == 1;
            case LIGHT -> config.entityCulling && !config.blockEntityCulling
                    && config.skipNearEntityCulling && config.nearEntityDistance == 12
                    && config.directionalEntityCulling && config.backgroundTasks
                    && config.shaderScanAsync && config.diagnostics && !config.fileLogging;
            case BALANCED -> config.entityCulling && config.blockEntityCulling
                    && config.skipNearEntityCulling && config.nearEntityDistance == 12
                    && config.directionalEntityCulling && config.backgroundTasks
                    && config.shaderScanAsync && config.diagnostics && config.fileLogging;
            case ADVANCED -> config.entityCulling && config.blockEntityCulling
                    && !config.skipNearEntityCulling && config.nearEntityDistance == 0
                    && config.directionalEntityCulling && config.backgroundTasks
                    && config.shaderScanAsync && config.diagnostics && config.fileLogging;
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseX >= mainX && mouseX <= mainX + mainW
                && mouseY >= contentTop && mouseY <= contentBottom && maxScroll > 0) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - vertical * 28));
            applyScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private void applyScroll() {
        for (Row row : rows) {
            int y = row.y() - (int) scroll;
            boolean visible = y + 36 >= contentTop && y <= contentBottom;
            row.hitbox().setY(y);
            row.hitbox().setVisible(visible);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);

        g.fill(0, 0, width, height, 0xE108111C);
        g.fill(0, 0, width, 2, 0xFF20D7C7);

        drawHeader(g);
        drawNavigation(g, mouseX, mouseY);
        drawMain(g, mouseX, mouseY);
        drawFooter(g);

        Row hover = hoveredRow(mouseX, mouseY);
        if (hover != null) drawTooltipPanel(g, hover);
    }

    private void drawHeader(GuiGraphicsExtractor g) {
        g.text(font, title, 16, 12, 0xFFF1F6FF, true);
        g.text(font, Component.translatable("superoptimizer.gui.subtitle"), 16, 27, 0xFF91A0B7, false);
    }

    private void drawNavigation(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.fill(navX, 46, navX + navW, height - 46, 0xB508111D);

        g.text(font, Component.literal("SuperOptimizer"), navX + 12, 52, 0xFFE8F2FF, true);
        g.text(font, Component.literal("0.2.4-alpha"), navX + 12, 66, 0xFF7E8BA2, false);

        for (NavButton item : nav) {
            int y = item.y();
            boolean selected = item.category() == category;
            boolean hovered = mouseX >= navX && mouseX <= navX + navW
                    && mouseY >= y && mouseY <= y + 34;

            g.fill(navX + 6, y, navX + navW - 6, y + 34,
                    selected ? 0xC51A3040 : hovered ? 0x8E132333 : 0x6B0C1724);
            if (selected) {
                g.fill(navX + 6, y, navX + 9, y + 34, 0xFF20D7C7);
            }

            String label = Component.translatable(item.category().key).getString();
            label = fit(label, navW - 34);
            g.text(font, Component.literal(label), navX + 16, y + 11,
                    selected ? 0xFF20E5D2 : 0xFFC7D3E5, selected);
        }
    }

    private void drawMain(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.fill(mainX, contentTop, mainX + mainW, contentBottom, 0xB8081321);

        String title = Component.translatable(category.key).getString();
        g.text(font, Component.literal("+ " + title), mainX + 12, contentTop + 10, 0xFFEAF2FF, true);

        int yStart = contentTop + 38;
        for (Row row : rows) {
            int y = row.y() - (int) scroll;
            if (y + 36 < yStart || y > contentBottom) continue;

            boolean hovered = mouseX >= mainX && mouseX <= mainX + mainW
                    && mouseY >= y && mouseY <= y + 36;

            g.fill(mainX, y, mainX + mainW, y + 34,
                    hovered ? 0xB81B2A3B : 0x86101928);

            String label = Component.translatable(row.key()).getString();
            label = label.replace(": %s", "");
            label = fit(label, mainW - 190);
            g.text(font, Component.literal(label), mainX + 12, y + 11,
                    hovered ? 0xFFEFF5FF : 0xFFD9E3F1, false);

            Component value = row.value().get();
            if (row.kind() == Kind.TOGGLE) {
                drawCheckbox(g, mainX + mainW - 28, y + 9, "ВКЛ".equals(value.getString()));
            } else {
                String right = fitRight(value.getString(), mainW - 170);
                int rw = font.width(right);
                g.text(font, Component.literal(right),
                        mainX + mainW - 16 - rw, y + 11,
                        row.kind() == Kind.STATUS ? 0xFF7FE1E7 : 0xFFE8EEF7,
                        row.kind() == Kind.PROFILE);
            }
        }
    }

    private void drawCheckbox(GuiGraphicsExtractor g, int x, int y, boolean checked) {
        g.fill(x, y, x + 16, y + 16, checked ? 0xFF20D7C7 : 0xFF5C6B80);
        g.fill(x + 3, y + 3, x + 13, y + 13, 0xD708111C);
        if (checked) g.fill(x + 5, y + 5, x + 11, y + 11, 0xFF20D7C7);
    }

    private void drawFooter(GuiGraphicsExtractor g) {
        g.fill(0, height - 42, width, height, 0xE008111B);
        g.text(font, Component.literal("F8 — открыть • Наведи на параметр — подробности"),
                navX + 8, height - 28, 0xFF7C8BA2, false);

        int doneW = Math.min(130, Math.max(96, mainW / 3));
        int x = mainX + mainW - doneW;
        g.fill(x, height - 34, x + doneW, height - 8, 0xB7173947);
        g.text(font, Component.translatable("gui.done"), x + (doneW / 2) - font.width(Component.translatable("gui.done")) / 2,
                height - 25, 0xFFEAF4FF, true);
    }

    private void drawTooltipPanel(GuiGraphicsExtractor g, Row row) {
        String text = Component.translatable(row.desc()).getString();
        if (text.isBlank()) return;

        int panelW = Math.min(360, Math.max(260, width / 3));
        int panelH = 52;
        int x = Math.min(width - panelW - 10, mainX + mainW + 8);
        int y = Math.max(50, contentBottom - panelH - 8);

        g.fill(x, y, x + panelW, y + panelH, 0xEF101C2B);
        g.fill(x, y, x + 3, y + panelH, 0xFF20D7C7);

        g.text(font, Component.literal("Описание"), x + 10, y + 9, 0xFF20D7C7, true);
        g.text(font, Component.literal(fit(text, panelW - 24)), x + 10, y + 27, 0xFFC4D0E1, false);
    }

    private Row hoveredRow(int mouseX, int mouseY) {
        for (Row row : rows) {
            int y = row.y() - (int) scroll;
            if (mouseX >= mainX && mouseX <= mainX + mainW
                    && mouseY >= y && mouseY <= y + 36) return row;
        }
        return null;
    }

    private String fit(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) return value;
        String ellipsis = "...";
        String s = value;
        while (s.length() > 1 && font.width(s + ellipsis) > maxWidth) {
            s = s.substring(0, s.length() - 1);
        }
        return s + ellipsis;
    }

    private String fitRight(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) return value;
        return fit(value, maxWidth);
    }

    @Override
    public void onClose() {
        config.save(minecraft.gameDirectory.toPath().resolve("config"));
        SuperOptimizerClient.applyConfig();
        minecraft.setScreenAndShow(parent);
    }
}
