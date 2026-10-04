package dev.kardipopo.memento;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class MementoScreen extends Screen {
    private final Screen parent;
    private List<MemoryRecord> records = List.of();
    private int selected = -1;
    private int scroll;

    public MementoScreen(Screen parent) {
        super(Component.literal("Memento — Архив мира"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        records = MemoryStore.all();

        addRenderableWidget(Button.builder(Component.literal("Сделать снимок"),
                b -> MemoryEngine.captureManual(Minecraft.getInstance()))
                .bounds(20, this.height - 42, 130, 28).build());

        addRenderableWidget(Button.builder(
                Component.literal(MemoryEngine.isArmed() ? "Автомоменты: ВКЛ" : "Автомоменты: ВЫКЛ"),
                b -> {
                    MemoryEngine.setArmed(!MemoryEngine.isArmed());
                    if (this.minecraft != null) this.minecraft.setScreenAndShow(new MementoScreen(parent));
                }).bounds(158, this.height - 42, 150, 28).build());

        addRenderableWidget(Button.builder(Component.literal("Обновить"),
                b -> {
                    if (this.minecraft != null) this.minecraft.setScreenAndShow(new MementoScreen(parent));
                }).bounds(316, this.height - 42, 100, 28).build());

        addRenderableWidget(Button.builder(Component.literal("Очистить архив"),
                b -> {
                    MemoryStore.clearAll();
                    if (this.minecraft != null) this.minecraft.setScreenAndShow(new MementoScreen(parent));
                }).bounds(this.width - 136, this.height - 42, 116, 28).build());

        addRenderableWidget(Button.builder(Component.literal("Закрыть"),
                b -> onClose()).bounds(this.width - 132, 18, 112, 26).build());
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF080D13);
        graphics.fill(0, 0, 5, this.height, 0xFF6DE7F2);
        graphics.drawString(this.font, Component.literal("MEMENTO"), 22, 18, 0xFFE9FCFF);
        graphics.drawString(this.font,
                Component.literal("Память мира — места и события, которые не хочется забыть."),
                22, 36, 0xFF8FA6B8);

        int listX = 20, listY = 64;
        int listW = Math.max(350, this.width / 2 - 30);
        int rowH = 42;
        int visible = Math.max(1, (this.height - 124) / rowH);

        records = MemoryStore.all();
        int maxScroll = Math.max(0, records.size() - visible);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        graphics.fill(listX, listY, listX + listW, this.height - 56, 0xFF101821);

        for (int i = 0; i < visible; i++) {
            int index = i + scroll;
            if (index >= records.size()) break;
            MemoryRecord r = records.get(index);
            int y = listY + i * rowH;
            boolean active = index == selected;
            graphics.fill(listX + 4, y + 4, listX + listW - 4, y + rowH - 2,
                    active ? 0xFF1C3942 : 0xFF151F29);
            graphics.drawString(this.font, Component.literal(r.event()),
                    listX + 12, y + 9, active ? 0xFFC8FBFF : 0xFFE5EEF3);
            graphics.drawString(this.font, Component.literal(r.timeText()),
                    listX + 12, y + 24, 0xFF718493);
            graphics.drawString(this.font, Component.literal(r.coordsText()),
                    listX + listW - 105, y + 16, 0xFF8499A9);
        }

        renderDetails(graphics, listX + listW + 18, listY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderDetails(GuiGraphicsExtractor graphics, int x, int y) {
        int w = this.width - x - 20;
        graphics.fill(x, y, x + w, this.height - 56, 0xFF101821);

        graphics.drawString(this.font, Component.literal("СОСТОЯНИЕ МИРА"),
                x + 14, y + 14, 0xFF78E8F5);
        graphics.drawString(this.font,
                Component.literal("Настроение: " + MemoryEngine.mood(Minecraft.getInstance())),
                x + 14, y + 34, 0xFFDDEBF1);
        graphics.drawString(this.font,
                Component.literal(MemoryEngine.currentContext(Minecraft.getInstance())),
                x + 14, y + 52, 0xFF8EA6B5);

        List<MemoryRecord> all = MemoryStore.all();
        long biomes = all.stream().map(MemoryRecord::biome).distinct().count();
        long dimensions = all.stream().map(MemoryRecord::dimension).distinct().count();

        graphics.drawString(this.font, Component.literal("Воспоминаний: " + all.size()),
                x + 14, y + 78, 0xFF8EA6B5);
        graphics.drawString(this.font, Component.literal("Биомов замечено: " + biomes),
                x + 14, y + 96, 0xFF8EA6B5);
        graphics.drawString(this.font, Component.literal("Измерений: " + dimensions),
                x + 14, y + 114, 0xFF8EA6B5);
        graphics.drawString(this.font, Component.literal("Автомоменты: " +
                (MemoryEngine.isArmed() ? "активны" : "пауза")),
                x + 14, y + 132, 0xFF8EA6B5);

        MemoryRecord selectedRecord = selected >= 0 && selected < records.size()
                ? records.get(selected) : MemoryEngine.latest();

        int dy = y + 168;
        graphics.drawString(this.font, Component.literal("ПОСЛЕДНЕЕ ВОСПОМИНАНИЕ"),
                x + 14, dy, 0xFF78E8F5);

        if (selectedRecord == null) {
            graphics.drawString(this.font, Component.literal("Архив пока пуст."),
                    x + 14, dy + 24, 0xFF788C9A);
            return;
        }

        graphics.drawString(this.font, Component.literal(selectedRecord.event()),
                x + 14, dy + 24, 0xFFEFFBFF);
        graphics.drawString(this.font, Component.literal(selectedRecord.timeText()),
                x + 14, dy + 42, 0xFF91A8B6);
        graphics.drawString(this.font, Component.literal("Мир: " + selectedRecord.world()),
                x + 14, dy + 60, 0xFF91A8B6);
        graphics.drawString(this.font, Component.literal("Измерение: " + selectedRecord.dimension()),
                x + 14, dy + 78, 0xFF91A8B6);
        graphics.drawString(this.font, Component.literal("Биом: " + selectedRecord.biome()),
                x + 14, dy + 96, 0xFF91A8B6);
        graphics.drawString(this.font, Component.literal("Координаты: " + selectedRecord.coordsText()),
                x + 14, dy + 114, 0xFF91A8B6);
        graphics.drawString(this.font, Component.literal(String.format(
                        java.util.Locale.ROOT, "Здоровье: %.1f", selectedRecord.health())),
                x + 14, dy + 132, 0xFF91A8B6);
        graphics.drawString(this.font, Component.literal("Снимок: " + selectedRecord.screenshot()),
                x + 14, dy + 150, 0xFF91A8B6);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int listW = Math.max(350, this.width / 2 - 30);
            int rowH = 42;
            int visible = Math.max(1, (this.height - 124) / rowH);
            if (mouseX >= 20 && mouseX < 20 + listW &&
                    mouseY >= 64 && mouseY < this.height - 56) {
                int index = (int)((mouseY - 64) / rowH) + scroll;
                if (index >= 0 && index < Math.min(records.size(), scroll + visible)) {
                    selected = index;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll -= (int)Math.signum(delta);
        int visible = Math.max(1, (this.height - 124) / 42);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, records.size() - visible)));
        return true;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreenAndShow(parent);
    }
}
