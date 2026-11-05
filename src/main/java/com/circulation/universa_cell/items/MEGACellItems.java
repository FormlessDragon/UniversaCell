package com.circulation.universa_cell.items;

import ae2.api.upgrades.Upgrades;
import ae2.core.definitions.ItemDefinition;
import ae2.core.localization.GuiText;
import ae2.items.storage.StorageTier;
import com.gripe.megacells.definition.MEGAItems;
import com.gripe.megacells.definition.MEGAStorageTiers;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.Item;

final class MEGACellItems {

    private static final ObjectArrayList<StorageTier> TIERS = ObjectArrayList.of(
        MEGAStorageTiers.TIER_1M,
        MEGAStorageTiers.TIER_4M,
        MEGAStorageTiers.TIER_16M,
        MEGAStorageTiers.TIER_64M,
        MEGAStorageTiers.TIER_256M);

    private static boolean registered;

    private MEGACellItems() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;

        CellItems.define("mega_universal_cell_housing", new Item());
        for (StorageTier tier : TIERS) {
            CellItems.UNIVERSAL_CELLS.add(CellItems.define(
                "universal_cell_" + tier.namePrefix(),
                new UniversalStorageCellItem(tier)));
            CellItems.PORTABLE_UNIVERSAL_CELLS.add(CellItems.define(
                "portable_universal_cell_" + tier.namePrefix(),
                new MEGAPortableUniversalStorageCellItem(tier)));
        }
    }

    static void registerUpgrades() {
        String portableCellGroup = GuiText.PortableCells.getTranslationKey();
        for (ItemDefinition<? extends PortableUniversalStorageCellItem> cell : CellItems.PORTABLE_UNIVERSAL_CELLS) {
            if (cell.item() instanceof MEGAPortableUniversalStorageCellItem) {
                Upgrades.add(MEGAItems.GREATER_ENERGY_CARD.item(), cell.item(), 2, portableCellGroup);
            }
        }
    }
}
