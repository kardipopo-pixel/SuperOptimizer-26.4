package dev.kardipopo.memento;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;

public final class MementoClient implements ClientModInitializer {
    public static final String MOD_ID = "memento";
    private static KeyMapping openKey;
    private static KeyMapping captureKey;

    @Override
    public void onInitializeClient() {
        Path root = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(MOD_ID);
        MemoryStore.init(root);

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main")
        );

        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.memento.open",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_F9,
                category
        ));

        captureKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.memento.capture",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_F10,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(MementoClient::tick);

        MemoryEngine.reset();
        MementoLog.info("Memento запущен.");
    }

    private static void tick(Minecraft client) {
        while (openKey.consumeClick()) {
            client.setScreenAndShow(new MementoScreen(null));
        }

        while (captureKey.consumeClick()) {
            MemoryEngine.captureManual(client);
        }

        MemoryEngine.tick(client);
    }

    public static void notifyPlayer(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gui.setOverlayMessage(Component.literal("§bMemento §8» §f" + message), false);
        }
    }

    public static Path root() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(MOD_ID);
    }
}
