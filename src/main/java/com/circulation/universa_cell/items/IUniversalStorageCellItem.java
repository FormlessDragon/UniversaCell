package com.circulation.universa_cell.items;

import ae2.api.storage.cells.ICellWorkbenchItem;
import ae2.api.storage.cells.IStackTooltipDataProvider;
import ae2.items.storage.StorageCellTooltipComponent;
import ae2.items.storage.StorageTier;
import com.circulation.universa_cell.utils.CellTooltipSupport;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Optional;

public interface IUniversalStorageCellItem extends ICellWorkbenchItem, IStackTooltipDataProvider {

    int MAX_TYPES = 63;

    StorageTier getTier();

    int getBytes(ItemStack stack);

    int getTotalTypes(ItemStack stack);

    int getBytesPerType(ItemStack stack);

    double getIdleDrain();

    default long getEffectiveBytes(ItemStack stack) {
        return getBytes(stack);
    }

    default long getEffectiveTotalTypes(ItemStack stack) {
        return getTotalTypes(stack);
    }

    default void addCellInformationToTooltip(ItemStack stack, List<String> lines) {
        CellTooltipSupport.addInformation(stack, this, lines);
    }

    @Override
    default Optional<StorageCellTooltipComponent> getStackTooltipData(ItemStack stack) {
        return CellTooltipSupport.create(stack, this);
    }
}
