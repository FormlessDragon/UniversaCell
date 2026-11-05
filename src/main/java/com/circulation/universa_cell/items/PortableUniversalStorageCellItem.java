package com.circulation.universa_cell.items;

import ae2.api.config.FuzzyMode;
import ae2.api.stacks.AEKeyTypes;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.UpgradeInventories;
import ae2.api.upgrades.Upgrades;
import ae2.container.GuiIds;
import ae2.core.AEConfig;
import ae2.items.contents.CellConfig;
import ae2.items.storage.StorageCellTooltipComponent;
import ae2.items.storage.StorageTier;
import ae2.items.tools.powered.AbstractPortableCell;
import ae2.util.ConfigInventory;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PortableUniversalStorageCellItem extends AbstractPortableCell implements IUniversalStorageCellItem {

    private static final int DEFAULT_COLOR = 0x80CCFF;
    private static final String FUZZY_MODE_TAG = "storage_cell_fuzzy_mode";

    private final StorageTier tier;

    public PortableUniversalStorageCellItem(StorageTier tier) {
        this(tier, AEConfig.instance().getPortableCellBattery(), DEFAULT_COLOR);
    }

    protected PortableUniversalStorageCellItem(StorageTier tier, double batteryCapacity, int defaultColor) {
        super(GuiIds.GuiKey.PORTABLE_ITEM_CELL, batteryCapacity, defaultColor);
        this.tier = tier;
        setCreativeTab(UniversalCellCreativeTab.TAB);
        setMaxStackSize(1);
    }

    @Override
    public StorageTier getTier() {
        return tier;
    }

    @Override
    public int getBytes(ItemStack stack) {
        return getTier().bytes() / 2;
    }

    @Override
    public int getTotalTypes(ItemStack stack) {
        return MAX_TYPES;
    }

    @Override
    public int getBytesPerType(ItemStack stack) {
        return getTier().bytes() / 128;
    }

    @Override
    public double getIdleDrain() {
        return 0.5;
    }

    @Override
    public double getChargeRate(ItemStack stack) {
        return 80 + 80 * Upgrades.getEnergyCardMultiplier(getUpgrades(stack));
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack stack) {
        return UpgradeInventories.forItem(stack, 3, this::onUpgradesChanged);
    }

    @Override
    public ConfigInventory getConfigInventory(ItemStack stack) {
        return CellConfig.create(AEKeyTypes.getAll(), stack, MAX_TYPES);
    }

    @Override
    public FuzzyMode getFuzzyMode(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey(FUZZY_MODE_TAG, 8)) {
            try {
                return FuzzyMode.valueOf(tag.getString(FUZZY_MODE_TAG));
            } catch (IllegalArgumentException ignored) {
                // Use AE2's default for invalid saved values.
            }
        }
        return FuzzyMode.IGNORE_ALL;
    }

    @Override
    public void setFuzzyMode(ItemStack stack, FuzzyMode fuzzyMode) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setString(FUZZY_MODE_TAG, fuzzyMode.name());
    }

    @Override
    public ResourceLocation getRecipeId() {
        ResourceLocation registryName = Objects.requireNonNull(
            getRegistryName(),
            "Portable cell has not been registered");
        return new ResourceLocation(registryName.getNamespace(), "tools/" + registryName.getPath());
    }

    @Override
    public void addCellInformationToTooltip(ItemStack stack, List<String> lines) {
        IUniversalStorageCellItem.super.addCellInformationToTooltip(stack, lines);
    }

    @Override
    public Optional<StorageCellTooltipComponent> getStackTooltipData(ItemStack stack) {
        return IUniversalStorageCellItem.super.getStackTooltipData(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addCheckedInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag flag) {
        super.addCheckedInformation(stack, world, lines, flag);
        addCellInformationToTooltip(stack, lines);
        PortableUniversalCellAutoPickup.addInformationToTooltip(stack, lines);
    }
}
