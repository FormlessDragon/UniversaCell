package com.circulation.universa_cell.utils;

import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.core.AEConfig;
import ae2.core.definitions.AEItems;
import ae2.core.localization.GuiText;
import ae2.core.localization.Tooltips;
import ae2.items.storage.StorageCellTooltipComponent;
import ae2.util.ConfigInventory;
import com.circulation.universa_cell.cells.UniversalCellHandler;
import com.circulation.universa_cell.cells.UniversalCellInventory;
import com.circulation.universa_cell.items.IUniversalStorageCellItem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class CellTooltipSupport {

    private CellTooltipSupport() {
    }

    public static void addInformation(
        ItemStack stack,
        IUniversalStorageCellItem cellItem,
        List<String> lines) {
        UniversalCellInventory inventory = UniversalCellHandler.INSTANCE.getCellInventory(stack, null);
        if (inventory == null) {
            return;
        }

        lines.add(Tooltips.bytesUsed(inventory.getUsedBytes(), inventory.getTotalBytes()).getFormattedText());
        lines.add(Tooltips.typesUsed(inventory.getStoredTypes(), inventory.getTotalTypes()).getFormattedText());

        ConfigInventory config = cellItem.getConfigInventory(stack);
        if (!config.isEmpty()) {
            IUpgradeInventory upgrades = cellItem.getUpgrades(stack);
            GuiText mode = upgrades.isInstalled(AEItems.INVERTER_CARD.item())
                ? GuiText.Excluded
                : GuiText.Included;
            GuiText precision = upgrades.isInstalled(AEItems.FUZZY_CARD.item())
                ? GuiText.Fuzzy
                : GuiText.Precise;
            lines.add(GuiText.Partitioned.getLocal() + " - " + mode.getLocal() + " " + precision.getLocal());
        }
    }

    public static Optional<StorageCellTooltipComponent> create(ItemStack stack, IUniversalStorageCellItem cellItem) {
        UniversalCellInventory inventory = UniversalCellHandler.INSTANCE.getCellInventory(stack, null);
        if (inventory == null) {
            return Optional.empty();
        }

        ObjectArrayList<ItemStack> upgrades = new ObjectArrayList<>();
        if (AEConfig.instance().isTooltipShowCellUpgrades()) {
            for (ItemStack upgrade : cellItem.getUpgrades(stack)) {
                if (!upgrade.isEmpty()) {
                    upgrades.add(upgrade.copy());
                }
            }
        }

        ObjectArrayList<GenericStack> contents = new ObjectArrayList<>();
        boolean hasMoreContent = false;
        if (AEConfig.instance().isTooltipShowCellContent()) {
            KeyCounter available = inventory.getAvailableStacks();
            available.forEach((key, amount) -> contents.add(new GenericStack(key, amount)));
            contents.sort(Comparator.comparingLong(GenericStack::amount)
                                    .reversed()
                                    .thenComparing(stackEntry -> stackEntry.what().getDisplayName().getFormattedText()));

            int maxShown = Math.max(0, AEConfig.instance().getTooltipMaxCellContentShown());
            hasMoreContent = contents.size() > maxShown;
            if (hasMoreContent) {
                contents.subList(maxShown, contents.size()).clear();
            }
        }

        return Optional.of(new StorageCellTooltipComponent(upgrades, contents, hasMoreContent, true));
    }
}
