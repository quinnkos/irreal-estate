package net.ndkoster.zillowmod.item;

import net.ndkoster.zillowmod.ZillowMod;
import net.ndkoster.zillowmod.item.custom.ZillowToolItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ZillowMod.MOD_ID);

    public static final RegistryObject<Item> ZILLOW_TOOL = ITEMS.register("zillow_tool",
            () -> new ZillowToolItem(new Item.Properties()));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
