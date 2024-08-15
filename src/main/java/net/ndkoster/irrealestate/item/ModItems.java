package net.ndkoster.irrealestate.item;

import net.ndkoster.irrealestate.IrrealEstate;
import net.ndkoster.irrealestate.item.custom.PropertyProfilerItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, IrrealEstate.MOD_ID);

    public static final RegistryObject<Item> PROPERTY_PROFILER = ITEMS.register("property-profiler",
            () -> new PropertyProfilerItem(new Item.Properties()));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
