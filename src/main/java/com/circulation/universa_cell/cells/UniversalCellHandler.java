package com.circulation.universa_cell.cells;

import ae2.api.storage.cells.ICellHandler;
import ae2.api.storage.cells.ISaveProvider;
import com.circulation.universa_cell.items.IUniversalStorageCellItem;
import net.minecraft.item.ItemStack;

public final class UniversalCellHandler implements ICellHandler {

    public static final UniversalCellHandler INSTANCE = new UniversalCellHandler();

    private UniversalCellHandler() {
    }

    @Override
    public boolean isCell(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IUniversalStorageCellItem;
    }

    @Override
    public UniversalCellInventory getCellInventory(ItemStack stack, ISaveProvider saveProvider) {
        if (!isCell(stack)) {
            return null;
        }
        return new UniversalCellInventory(stack, (IUniversalStorageCellItem) stack.getItem(), saveProvider);
    }
}
