package dev.kardipopo.superoptimizer;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;

/** Adds a direct SuperOptimizer entry to Minecraft's Video/Graphics Settings screen. */
public final class GraphicsSettingsIntegration {
    private static final String BUTTON_TEXT = "superoptimizer.button.open_graphics";

    private GraphicsSettingsIntegration() {}

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof VideoSettingsScreen)) return;

            for (AbstractWidget widget : Screens.getWidgets(screen)) {
                if (widget instanceof Button button
                        && button.getMessage().getString().equals(Component.translatable(BUTTON_TEXT).getString())) {
                    return;
                }
            }

            int w = Math.min(220, Math.max(170, scaledWidth / 4));
            int h = 20;
            int x = Math.max(8, scaledWidth / 2 - w - 6);
            int y = Math.max(8, scaledHeight - 28);

            Button button = Button.builder(
                    Component.translatable(BUTTON_TEXT),
                    b -> client.setScreenAndShow(new SuperOptimizerScreen(screen, SuperOptimizerClient.config()))
            ).bounds(x, y, w, h).build();

            Screens.getWidgets(screen).add(button);
        });
    }
}
