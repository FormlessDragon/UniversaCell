package com.circulation.universa_cell.items;

import com.circulation.universa_cell.Tags;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class UniversalCellCreativeTab {

    public static final CreativeTabs TAB = new CreativeTabs(Tags.MOD_ID) {
        @Override
        @NotNull
        public ItemStack createIcon() {
            return CellItems.UNIVERSAL_CELL_HOUSING.stack();
        }
    };

    private UniversalCellCreativeTab() {
    }
}
