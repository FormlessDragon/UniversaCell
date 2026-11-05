package com.circulation.universa_cell.items;

import ae2.api.client.StorageCellModels;
import ae2.core.definitions.ItemDefinition;
import ae2.items.storage.StorageTier;
import com.circulation.universa_cell.Tags;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class CellClientRegistration {

    private CellClientRegistration() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        for (Item item : CellItems.allItems()) {
            ModelLoader.setCustomModelResourceLocation(
                item,
                0,
                new ModelResourceLocation(
                    Objects.requireNonNull(item.getRegistryName(), "Item has not been registered"),
                    "inventory"));
        }

        for (ItemDefinition<UniversalStorageCellItem> cell : CellItems.UNIVERSAL_CELLS) {
            UniversalStorageCellItem item = Objects.requireNonNull(cell.item(), "Cell item has not been registered");
            registerDriveModel(item, item.getTier());
        }
        for (ItemDefinition<? extends PortableUniversalStorageCellItem> cell : CellItems.PORTABLE_UNIVERSAL_CELLS) {
            PortableUniversalStorageCellItem item = Objects.requireNonNull(
                cell.item(),
                "Cell item has not been registered");
            registerDriveModel(item, item.getTier());
        }
    }

    private static void registerDriveModel(Item item, StorageTier tier) {
        StorageCellModels.registerModel(item, new ResourceLocation(
            Tags.MOD_ID,
            "block/drive/cells/" + tier.namePrefix() + "_universal_cell"));
    }

}
