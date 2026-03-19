package com.happysg.radar.registry;

import com.happysg.radar.CreateRadar;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModCreativeTabs {

    public static final ItemGroup RADAR_TAB = Registry.register(
            Registries.ITEM_GROUP,
            new Identifier(CreateRadar.MODID, "radar"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.create_radar.radar"))
                    .icon(() -> {
                        Item icon = blockItem(ModBlocks.MONITOR);
                        return icon == Items.AIR ? ItemStack.EMPTY : new ItemStack(icon);
                    })
                    .build()
    );

    public static void register() {
        CreateRadar.LOGGER.info("Registering Creative Tabs!");
        RegistryKey<ItemGroup> tabKey = RegistryKey.of(Registries.ITEM_GROUP.getKey(), new Identifier(CreateRadar.MODID, "radar"));
        ItemGroupEvents.modifyEntriesEvent(tabKey).register(entries -> {
            addBlock(entries, ModBlocks.MONITOR);
            entries.add(ModItems.SAFE_ZONE_DESIGNATOR.get());
            addBlock(entries, ModBlocks.RADAR_BEARING_BLOCK);
            addBlock(entries, ModBlocks.RADAR_RECEIVER_BLOCK);
            addBlock(entries, ModBlocks.RADAR_PLATE_BLOCK);
            addBlock(entries, ModBlocks.RADAR_DISH_BLOCK);
            addBlock(entries, ModBlocks.CREATIVE_RADAR_PLATE_BLOCK);
            addBlock(entries, ModBlocks.NETWORK_FILTERER_BLOCK);
            entries.add(ModItems.IDENT_FILTER_ITEM.get());
            entries.add(ModItems.RADAR_FILTER_ITEM.get());
            entries.add(ModItems.TARGET_FILTER_ITEM.get());
            entries.add(ModItems.BINOCULARS.get());
        });
    }

    /** Look up the block item directly from Registries.ITEM (avoids Block.asItem() / BLOCK_ITEMS map). */
    private static Item blockItem(BlockEntry<?> entry) {
        Identifier id = Registries.BLOCK.getId(entry.get());
        return Registries.ITEM.get(new Identifier(id.getNamespace(), id.getPath()));
    }

    private static void addBlock(ItemGroup.Entries entries, BlockEntry<?> entry) {
        Item item = blockItem(entry);
        if (item != Items.AIR) entries.add(item);
    }
}
