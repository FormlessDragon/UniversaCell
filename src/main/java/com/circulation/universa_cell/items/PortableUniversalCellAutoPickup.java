package com.circulation.universa_cell.items;

import ae2.api.config.Actionable;
import ae2.api.config.PowerMultiplier;
import ae2.api.features.HotkeyAction;
import ae2.api.networking.energy.IEnergySource;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEItemKey;
import ae2.api.storage.StorageCells;
import ae2.api.storage.StorageHelper;
import ae2.api.storage.cells.StorageCell;
import ae2.client.Hotkeys;
import ae2.core.localization.GuiText;
import ae2.core.localization.PlayerMessages;
import ae2.integration.modules.baubles.BaublesIntegration;
import ae2.items.tools.powered.PortableItemCellAutoPickup;
import ae2.me.helpers.PlayerSource;
import ae2.util.Platform;
import com.circulation.universa_cell.Tags;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class PortableUniversalCellAutoPickup {

    private static final String AUTO_PICKUP_TAG = "portable_item_cell_auto_pickup";

    private PortableUniversalCellAutoPickup() {
    }

    public static void registerHotkey() {
        HotkeyAction.register(PortableUniversalCellAutoPickup::toggleFirstAvailable,
                PortableItemCellAutoPickup.HOTKEY_ID);
    }

    @SideOnly(Side.CLIENT)
    public static void addInformationToTooltip(ItemStack stack, List<String> lines) {
        String hotkey = Hotkeys.getHotkeyDisplayName(PortableItemCellAutoPickup.HOTKEY_ID);
        if (hotkey == null || hotkey.isEmpty()) {
            hotkey = GuiText.PortableItemCellAutoPickupHotkeyUnbound.getLocal();
        }
        String enabled = isEnabled(stack) ? GuiText.Yes.getLocal() : GuiText.No.getLocal();
        lines.add(GuiText.PortableItemCellAutoPickupTooltip.getLocal(enabled, hotkey));
    }

    private static boolean toggleFirstAvailable(EntityPlayerMP player) {
        for (ItemStack stack : player.inventory.mainInventory) {
            if (toggle(stack, player)) {
                return true;
            }
        }
        for (int slot = 0; slot < BaublesIntegration.getSlots(player); slot++) {
            if (toggle(BaublesIntegration.getStackInSlot(player, slot), player)) {
                return true;
            }
        }
        return false;
    }

    private static boolean toggle(ItemStack stack, EntityPlayerMP player) {
        if (!(stack.getItem() instanceof PortableUniversalStorageCellItem)) {
            return false;
        }
        boolean enabled = !isEnabled(stack);
        Platform.openNbtData(stack).setBoolean(AUTO_PICKUP_TAG, enabled);
        player.sendStatusMessage((enabled
                ? PlayerMessages.PortableItemCellAutoPickupEnabled
                : PlayerMessages.PortableItemCellAutoPickupDisabled).text(), true);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntityPlayer() instanceof EntityPlayerMP player)) {
            return;
        }

        EntityItem entityItem = event.getItem();
        ItemStack dropped = entityItem.getItem();
        AEItemKey key = AEItemKey.of(dropped);
        if (key == null) {
            return;
        }

        IActionSource source = new PlayerSource(player);
        for (ItemStack cellStack : player.inventory.mainInventory) {
            if (tryInsert(player, entityItem, dropped, key, cellStack, source)) {
                event.setCanceled(true);
                return;
            }
        }
        for (int slot = 0; slot < BaublesIntegration.getSlots(player); slot++) {
            if (tryInsert(player, entityItem, dropped, key, BaublesIntegration.getStackInSlot(player, slot), source)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    private static boolean tryInsert(
            EntityPlayerMP player,
            EntityItem entityItem,
            ItemStack dropped,
            AEItemKey key,
            ItemStack cellStack,
            IActionSource source) {
        if (!(cellStack.getItem() instanceof PortableUniversalStorageCellItem cellItem)
                || !isEnabled(cellStack)) {
            return false;
        }

        StorageCell cell = StorageCells.getCellInventory(cellStack, null);
        if (cell == null) {
            return false;
        }

        int before = dropped.getCount();
        long inserted = StorageHelper.poweredInsert(
                new PortableEnergySource(cellStack, cellItem),
                cell,
                key,
                before,
                source,
                Actionable.MODULATE);
        if (inserted <= 0) {
            return false;
        }

        cell.persist();
        int insertedCount = Math.toIntExact(Math.min(inserted, before));
        dropped.shrink(insertedCount);
        StatBase pickupStat = StatList.getObjectsPickedUpStats(key.getItem());
        if (pickupStat != null) {
            player.addStat(pickupStat, insertedCount);
        }
        player.onItemPickup(entityItem, insertedCount);
        entityItem.setItem(dropped);
        if (dropped.isEmpty()) {
            entityItem.setDead();
            return true;
        }
        return false;
    }

    private record PortableEnergySource(ItemStack stack, PortableUniversalStorageCellItem cell)
            implements IEnergySource {

        @Override
        public double extractAEPower(double amount, Actionable mode, PowerMultiplier multiplier) {
            return multiplier.divide(cell.extractAEPower(stack, multiplier.multiply(amount), mode));
        }
    }

    private static boolean isEnabled(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.getBoolean(AUTO_PICKUP_TAG);
    }
}
