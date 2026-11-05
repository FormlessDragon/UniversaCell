package com.circulation.universa_cell.items;

import ae2.core.AEConfig;
import ae2.items.storage.StorageTier;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.Objects;

public final class MEGAPortableUniversalStorageCellItem extends PortableUniversalStorageCellItem {

    private static final double BATTERY_MULTIPLIER = 8;
    private static final int DEFAULT_COLOR = 0x80CBFF;

    public MEGAPortableUniversalStorageCellItem(StorageTier tier) {
        super(tier, AEConfig.instance().getPortableCellBattery() * BATTERY_MULTIPLIER, DEFAULT_COLOR);
    }

    @Override
    public double getIdleDrain() {
        return 1;
    }

    @Override
    public double getChargeRate(ItemStack stack) {
        return super.getChargeRate(stack) * 2;
    }

    @Override
    public ResourceLocation getRecipeId() {
        ResourceLocation registryName = Objects.requireNonNull(
            getRegistryName(),
            "Portable cell has not been registered");
        return new ResourceLocation(registryName.getNamespace(), "cells/portable/" + registryName.getPath());
    }
}
