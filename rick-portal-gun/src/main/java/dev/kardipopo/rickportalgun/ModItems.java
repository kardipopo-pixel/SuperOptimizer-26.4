package dev.kardipopo.rickportalgun;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {
    public static final Item PORTAL_GUN = Registry.register(
            Registries.ITEM,
            Identifier.of(RickPortalGun.MOD_ID, "portal_gun"),
            new PortalGunItem(new Item.Settings().maxCount(1))
    );

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(PORTAL_GUN));
    }
}
