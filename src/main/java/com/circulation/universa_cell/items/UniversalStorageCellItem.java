package com.circulation.universa_cell.items;

import ae2.api.config.FuzzyMode;
import ae2.api.stacks.AEKeyTypes;
import ae2.api.storage.StorageCells;
import ae2.api.storage.cells.StorageCell;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.UpgradeInventories;
import ae2.core.localization.PlayerMessages;
import ae2.items.AEBaseItem;
import ae2.items.contents.CellConfig;
import ae2.items.storage.StorageTier;
import ae2.recipes.game.StorageCellDisassemblyRecipe;
import ae2.util.ConfigInventory;
import ae2.util.InteractionUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UniversalStorageCellItem extends AEBaseItem implements IUniversalStorageCellItem {

    private static final String FUZZY_MODE_TAG = "storage_cell_fuzzy_mode";

    private final StorageTier tier;

    public UniversalStorageCellItem(StorageTier tier) {
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
        return getTier().bytes();
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
        return getTier().idleDrain();
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack stack) {
        return UpgradeInventories.forItem(stack, 4);
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
                // Keep the same fallback as AE2 when a saved fuzzy mode is invalid.
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
    protected void addCheckedInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag flag) {
        addCellInformationToTooltip(stack, lines);
    }

    @Override
    public @NotNull ActionResult<ItemStack> onItemRightClick(@NotNull World world, @NotNull EntityPlayer player, @NotNull EnumHand hand) {
        disassembleCell(player.getHeldItem(hand), world, player);
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public @NotNull EnumActionResult onItemUseFirst(
        @NotNull EntityPlayer player,
        @NotNull World world,
        @NotNull BlockPos pos,
        @NotNull EnumFacing side,
        float hitX,
        float hitY,
        float hitZ,
        @NotNull EnumHand hand) {
        return disassembleCell(player.getHeldItem(hand), world, player) ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
    }

    private boolean disassembleCell(ItemStack stack, World world, EntityPlayer player) {
        if (!InteractionUtil.isInAlternateUseMode(player)) {
            return false;
        }

        List<ItemStack> disassembledStacks = StorageCellDisassemblyRecipe.getDisassemblyResult(world, stack.getItem());
        InventoryPlayer inventory = player.inventory;
        if (disassembledStacks.isEmpty() || inventory.getCurrentItem() != stack) {
            return false;
        }

        StorageCell cell = StorageCells.getCellInventory(stack, null);
        if (cell != null && !cell.getAvailableStacks().isEmpty()) {
            player.sendStatusMessage(PlayerMessages.OnlyEmptyCellsCanBeDisassembled.text(), true);
            return false;
        }

        inventory.setInventorySlotContents(inventory.currentItem, ItemStack.EMPTY);
        for (ItemStack disassembledStack : disassembledStacks) {
            inventory.placeItemBackInInventory(world, disassembledStack.copy());
        }
        getUpgrades(stack).forEach(upgrade -> inventory.placeItemBackInInventory(world, upgrade));
        return true;
    }
}
