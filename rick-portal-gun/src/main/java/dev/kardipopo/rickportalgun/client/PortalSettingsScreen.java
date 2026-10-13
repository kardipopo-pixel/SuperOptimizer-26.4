package dev.kardipopo.rickportalgun.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class PortalSettingsScreen extends Screen {
    private static final String[] DIMENSIONS = {
            "minecraft:overworld",
            "minecraft:the_nether",
            "minecraft:the_end"
    };
    private static int selectedDimension = 0;
    private static boolean destinationMode = false;
    private static String savedX = "0";
    private static String savedY = "80";
    private static String savedZ = "0";

    private final Screen parent;
    private TextFieldWidget xField;
    private TextFieldWidget yField;
    private TextFieldWidget zField;
    private ButtonWidget modeButton;
    private ButtonWidget dimensionButton;
    private String status = "Сохрани настройки, чтобы отправить их серверу.";

    public PortalSettingsScreen(Screen parent) {
        super(Text.literal("Портальная пушка C-137"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        int top = this.height / 2 - 84;

        modeButton = this.addDrawableChild(ButtonWidget.builder(modeText(), button -> {
            destinationMode = !destinationMode;
            button.setMessage(modeText());
        }).dimensions(center - 120, top, 240, 20).build());

        dimensionButton = this.addDrawableChild(ButtonWidget.builder(dimensionText(), button -> {
            selectedDimension = (selectedDimension + 1) % DIMENSIONS.length;
            button.setMessage(dimensionText());
        }).dimensions(center - 120, top + 26, 240, 20).build());

        int fieldY = top + 70;
        xField = this.addDrawableChild(new TextFieldWidget(this.textRenderer, center - 115, fieldY, 68, 20, Text.literal("X")));
        yField = this.addDrawableChild(new TextFieldWidget(this.textRenderer, center - 34, fieldY, 68, 20, Text.literal("Y")));
        zField = this.addDrawableChild(new TextFieldWidget(this.textRenderer, center + 47, fieldY, 68, 20, Text.literal("Z")));
        xField.setText(savedX);
        yField.setText(savedY);
        zField.setText(savedZ);
        xField.setMaxLength(10);
        yField.setMaxLength(5);
        zField.setMaxLength(10);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Сохранить"), button -> save())
                .dimensions(center - 120, top + 112, 116, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), button -> close())
                .dimensions(center + 4, top + 112, 116, 20).build());
    }

    private Text modeText() {
        return Text.literal(destinationMode ? "Режим: координаты / измерение" : "Режим: парные порталы");
    }

    private Text dimensionText() {
        return Text.literal("Измерение: " + DIMENSIONS[selectedDimension] + "  »");
    }

    private void save() {
        try {
            int x = Integer.parseInt(xField.getText().trim());
            int y = Integer.parseInt(yField.getText().trim());
            int z = Integer.parseInt(zField.getText().trim());
            if (y < -64 || y > 320) {
                status = "Y должен быть в диапазоне от -64 до 320.";
                return;
            }
            savedX = Integer.toString(x);
            savedY = Integer.toString(y);
            savedZ = Integer.toString(z);

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.getNetworkHandler() == null) {
                status = "Сначала войди в мир.";
                return;
            }
            String command = "rick config " + DIMENSIONS[selectedDimension] + " " + x + " " + y + " " + z + " " + destinationMode;
            client.getNetworkHandler().sendChatCommand(command);
            status = "Отправлено серверу.";
            close();
        } catch (NumberFormatException exception) {
            status = "Введи целые числа в поля X, Y и Z.";
        }
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("ПОРТАЛЬНАЯ ПУШКА C-137"), this.width / 2, this.height / 2 - 105, 0x55FF77);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("F8 открывает меню только когда пушка в руке"), this.width / 2, this.height / 2 - 95, 0xAAAAAA);
        int center = this.width / 2;
        int top = this.height / 2 - 14;
        context.drawTextWithShadow(this.textRenderer, "X", center - 115, top + 7, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "Y", center - 34, top + 7, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "Z", center + 47, top + 7, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(destinationMode ? "При режиме координат порталы ведут прямо в выбранную точку." : "В этом режиме второй портал будет парой первого."),
                center, top + 32, 0xDDDDDD);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(status), center, top + 90, 0xAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
