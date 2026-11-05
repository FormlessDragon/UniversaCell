package com.circulation.universa_cell.cells;

import ae2.api.storage.StorageCells;
import ae2.api.upgrades.Upgrades;
import ae2.core.definitions.AEItems;
import ae2.core.definitions.ItemDefinition;
import ae2.core.localization.GuiText;
import com.circulation.universa_cell.items.CellItems;
import com.circulation.universa_cell.items.MEGAPortableUniversalStorageCellItem;
import com.circulation.universa_cell.items.PortableUniversalStorageCellItem;
import com.circulation.universa_cell.items.PortableUniversalCellAutoPickup;
import com.circulation.universa_cell.items.UniversalStorageCellItem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.Item;

public final class CellIntegration {

    private CellIntegration() {
    }

    public static void initialize() {
        StorageCells.addCellHandler(UniversalCellHandler.INSTANCE);
        registerUpgrades();
        CellItems.registerOptionalUpgrades();
        PortableUniversalCellAutoPickup.registerHotkey();
    }

    private static void registerUpgrades() {
        String storageCellGroup = GuiText.StorageCells.getTranslationKey();
        String portableCellGroup = GuiText.PortableCells.getTranslationKey();
        ObjectArrayList<Item> supportedUpgrades = ObjectArrayList.of(
            AEItems.FUZZY_CARD.item(),
            AEItems.INVERTER_CARD.item(),
            AEItems.EQUAL_DISTRIBUTION_CARD.item(),
            AEItems.STICKY_CARD.item(),
            AEItems.VOID_CARD.item());

        for (ItemDefinition<UniversalStorageCellItem> definition : CellItems.UNIVERSAL_CELLS) {
            Item cell = definition.item();
            for (Item upgrade : supportedUpgrades) {
                Upgrades.add(upgrade, cell, 1, storageCellGroup);
            }
        }

        for (ItemDefinition<? extends PortableUniversalStorageCellItem> definition : CellItems.PORTABLE_UNIVERSAL_CELLS) {
            Item cell = definition.item();
            for (Item upgrade : supportedUpgrades) {
                Upgrades.add(upgrade, cell, 1, portableCellGroup);
            }
            if (!(cell instanceof MEGAPortableUniversalStorageCellItem)) {
                Upgrades.add(AEItems.ENERGY_CARD.item(), cell, 2, portableCellGroup);
            }
        }
    }
}
