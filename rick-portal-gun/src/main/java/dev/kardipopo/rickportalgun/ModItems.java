package dev.kardipopo.rickportalgun;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModItems {
    private static final Identifier PORTAL_GUN_ID = Identifier.of(RickPortalGun.MOD_ID, "portal_gun");

    public static final Item PORTAL_GUN = Registry.register(
            Registries.ITEM,
            PORTAL_GUN_ID,
            new PortalGunItem(new Item.Settings()
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, PORTAL_GUN_ID))
                    .maxCount(1))
    );

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(PORTAL_GUN));
    }
}
