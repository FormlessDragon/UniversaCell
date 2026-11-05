package com.circulation.universa_cell.cells;

import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.AEKey2LongMap;
import ae2.api.stacks.AEKeyTypes;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import ae2.api.storage.MEStorageChangeListener;
import ae2.api.storage.cells.CellState;
import ae2.api.storage.cells.ISaveProvider;
import ae2.api.storage.cells.StorageCell;
import ae2.api.storage.cells.StorageCellStatistics;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.core.AELog;
import ae2.core.definitions.AEItems;
import ae2.text.TextComponentItemStack;
import ae2.util.CellWorkbenchFilter;
import ae2.util.prioritylist.IPartitionList;
import com.circulation.universa_cell.items.IUniversalStorageCellItem;
import com.circulation.universa_cell.utils.CellStorageMath;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.Objects;

public final class UniversalCellInventory implements StorageCell, StorageCellStatistics {

    private static final String INVENTORY_TAG = "storage_cell_inv";
    private static final String PREVIOUS_INVENTORY_TAG = "universacell_inventory";
    private static final String LEGACY_ITEMS_TAG = "it";

    private final ItemStack itemStack;
    private final IUniversalStorageCellItem cellItem;
    private final long capacityBytes;
    private final long maxTypes;
    private final int bytesPerType;
    private final ISaveProvider saveProvider;
    private final AEKey2LongMap cellItems = new AEKey2LongMap.SaturatingOpenHashMap();
    private final ObjectArrayList<ListenerRegistration> listeners = new ObjectArrayList<>();
    private final boolean partitionInverted;
    private final boolean partitionFuzzy;
    private final IPartitionList partitionList;
    private boolean persisted = true;
    private boolean dispatchingListeners;
    private boolean listUpdateRequired;
    private ListenerRegistration currentListener;

    UniversalCellInventory(ItemStack itemStack, IUniversalStorageCellItem cellItem, ISaveProvider saveProvider) {
        this.itemStack = itemStack;
        this.cellItem = cellItem;
        this.capacityBytes = cellItem.getEffectiveBytes(itemStack);
        this.maxTypes = cellItem.getEffectiveTotalTypes(itemStack);
        this.bytesPerType = cellItem.getBytesPerType(itemStack);
        this.saveProvider = saveProvider;

        this.partitionInverted = CellWorkbenchFilter.isInverted(itemStack, cellItem);
        this.partitionFuzzy = CellWorkbenchFilter.isFuzzy(itemStack, cellItem);
        this.partitionList = CellWorkbenchFilter.createPartitionList(itemStack, cellItem, partitionFuzzy);
        loadCellItems();
    }

    private void loadCellItems() {
        NBTTagCompound tag = itemStack.getTagCompound();
        if (tag == null) {
            return;
        }
        if (tag.hasKey(INVENTORY_TAG, 9)) {
            loadGenericStacks(tag.getTagList(INVENTORY_TAG, 10));
        } else if (tag.hasKey(PREVIOUS_INVENTORY_TAG, 9)) {
            loadGenericStacks(tag.getTagList(PREVIOUS_INVENTORY_TAG, 10));
            persisted = false;
        } else if (tag.hasKey(LEGACY_ITEMS_TAG, 9)) {
            loadLegacyStacks(tag.getTagList(LEGACY_ITEMS_TAG, 10));
            persisted = false;
        }
    }

    private void loadGenericStacks(NBTTagList stacks) {
        for (GenericStack stack : GenericStack.readList(stacks)) {
            if (stack != null) {
                addLoadedStack(stack.what(), stack.amount());
            }
        }
    }

    private void loadLegacyStacks(NBTTagList stacks) {
        for (int i = 0; i < stacks.tagCount(); i++) {
            NBTTagCompound stack = stacks.getCompoundTagAt(i);
            try {
                AEKey key = AEKeyTypes.get(new ResourceLocation(stack.getString("t")))
                        .loadKeyFromTag(stack.getCompoundTag("k"));
                addLoadedStack(key, stack.getLong("a"));
            } catch (RuntimeException ignored) {
                // Ignore malformed legacy entries and preserve valid cell contents.
            }
        }
    }

    private void addLoadedStack(AEKey key, long amount) {
        if (key != null && amount > 0) {
            cellItems.addTo(key, amount);
        }
    }

    @Override
    public long insert(AEKey key, long amount, Actionable mode, IActionSource source) {
        MEStorage.checkPreconditions(key, amount, mode, source);
        if (amount == 0 || !isAllowedByCellWorkbench(key)) {
            return 0;
        }

        long oldAmount = cellItems.getLong(key);
        boolean newType = oldAmount == 0;
        boolean typeLimitReached = newType && cellItems.size() >= maxTypes;
        long accepted = typeLimitReached ? 0 : maxInsertable(key, amount, oldAmount);
        IUpgradeInventory upgrades = cellItem.getUpgrades(itemStack);
        boolean voidOverflow = upgrades.isInstalled(AEItems.VOID_CARD.item());
        long reportedAccepted = voidOverflow ? amount : accepted;
        if (accepted <= 0 || mode != Actionable.MODULATE) {
            return reportedAccepted;
        }

        cellItems.addTo(key, accepted);
        saveChanges();
        notifyStackChange(key, accepted, newType);
        return reportedAccepted;
    }

    private long maxInsertable(AEKey key, long requested, long oldAmount) {
        long low = 0;
        long maxAmount = maxAmountPerType(key);
        long remainingForType = Math.clamp(maxAmount - oldAmount, 0L, Long.MAX_VALUE - oldAmount);
        long high = Math.clamp(requested, 0L, remainingForType);
        while (low < high) {
            long midpoint = low + ((high - low) >>> 1) + 1;
            long candidate = CellStorageMath.saturatedAdd(oldAmount, midpoint);
            if (usedBytesWith(key, candidate) <= capacityBytes) {
                low = midpoint;
            } else {
                high = midpoint - 1;
            }
        }
        return low;
    }

    private long maxAmountPerType(AEKey key) {
        IUpgradeInventory upgrades = cellItem.getUpgrades(itemStack);
        if (!upgrades.isInstalled(AEItems.EQUAL_DISTRIBUTION_CARD.item())) {
            return Long.MAX_VALUE;
        }

        long distributedTypes = maxTypes;
        if (!partitionFuzzy && !partitionInverted && !partitionList.isEmpty()) {
            long partitionedTypes = 0;
            for (AEKey ignored : partitionList.getItems()) {
                partitionedTypes++;
            }
            distributedTypes = Math.min(distributedTypes, partitionedTypes);
        }

        return CellStorageMath.maxAmountPerType(
                capacityBytes,
                distributedTypes,
                bytesPerType,
                key.getType().getAmountPerByte());
    }

    private long usedBytesWith(AEKey replacementKey, long replacementAmount) {
        AEKey2LongMap amountsByKey = new AEKey2LongMap.OpenHashMap(cellItems);
        amountsByKey.put(replacementKey, replacementAmount);
        long typeCount = cellItems.size() + (cellItems.containsKey(replacementKey) ? 0 : 1);
        return CellStorageMath.usedBytes(
            amountsByKey,
            key -> key.getType().getAmountPerByte(),
            typeCount,
            bytesPerType);
    }

    @Override
    public long extract(AEKey key, long amount, Actionable mode, IActionSource source) {
        MEStorage.checkPreconditions(key, amount, mode, source);
        if (amount == 0) {
            return 0;
        }

        long existing = cellItems.getLong(key);
        long extracted = Math.min(existing, amount);
        if (extracted <= 0 || mode != Actionable.MODULATE) {
            return extracted;
        }

        long remainder = existing - extracted;
        if (remainder == 0) {
            cellItems.removeLong(key);
        } else {
            cellItems.addTo(key, -extracted);
        }
        saveChanges();
        notifyStackChange(key, -extracted, remainder == 0);
        return extracted;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        cellItems.forEachEntry(out::add);
    }

    @Override
    public ITextComponent getDescription() {
        return TextComponentItemStack.of(itemStack);
    }

    @Override
    public CellState getStatus() {
        if (cellItems.isEmpty()) {
            return CellState.EMPTY;
        }
        long usedBytes = getUsedBytes();
        long remainingBytes = capacityBytes - usedBytes;
        if (remainingBytes <= 0) {
            return CellState.FULL;
        }
        if (cellItems.size() >= maxTypes || remainingBytes <= bytesPerType) {
            return CellState.TYPES_FULL;
        }
        return CellState.NOT_EMPTY;
    }

    @Override
    public double getIdleDrain() {
        return cellItem.getIdleDrain();
    }

    @Override
    public boolean canFitInsideCell() {
        return cellItems.isEmpty();
    }

    @Override
    public boolean isStickyStorageFor(AEKey key, IActionSource source) {
        IUpgradeInventory upgrades = cellItem.getUpgrades(itemStack);
        if (!upgrades.isInstalled(AEItems.STICKY_CARD.item())) {
            return false;
        }
        return partitionList.isEmpty() ? cellItems.containsKey(key) : isAllowedByCellWorkbench(key);
    }

    @Override
    public void persist() {
        if (persisted) {
            return;
        }

        NBTTagCompound tag = itemStack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            itemStack.setTagCompound(tag);
        }

        if (cellItems.isEmpty()) {
            tag.removeTag(INVENTORY_TAG);
        } else {
            ObjectArrayList<GenericStack> serialized = new ObjectArrayList<>(cellItems.size());
            cellItems.forEachEntry((key, amount) -> serialized.add(new GenericStack(key, amount)));
            tag.setTag(INVENTORY_TAG, GenericStack.writeList(serialized));
        }
        tag.removeTag(PREVIOUS_INVENTORY_TAG);
        tag.removeTag(LEGACY_ITEMS_TAG);
        if (tag.getKeySet().isEmpty()) {
            itemStack.setTagCompound(null);
        }
        persisted = true;
    }

    @Override
    public void addListener(MEStorageChangeListener listener, Object verificationToken) {
        Objects.requireNonNull(listener, "listener");
        for (ListenerRegistration registration : listeners) {
            if (registration.listener == listener) {
                throw new IllegalStateException("The storage listener is already registered.");
            }
        }
        listeners.add(new ListenerRegistration(listener, verificationToken));
    }

    @Override
    public void removeListener(MEStorageChangeListener listener) {
        for (int i = listeners.size() - 1; i >= 0; i--) {
            ListenerRegistration registration = listeners.get(i);
            if (registration.listener == listener) {
                registration.active = false;
                if (!dispatchingListeners) {
                    listeners.remove(i);
                }
            }
        }
    }

    @Override
    public long getUsedBytes() {
        return CellStorageMath.usedBytes(
            cellItems,
            key -> key.getType().getAmountPerByte(),
            cellItems.size(),
            bytesPerType);
    }

    @Override
    public long getTotalBytes() {
        return capacityBytes;
    }

    @Override
    public long getStoredTypes() {
        return cellItems.size();
    }

    @Override
    public long getTotalTypes() {
        return maxTypes;
    }

    @Override
    public int getBytesPerType() {
        return bytesPerType;
    }

    private boolean isAllowedByCellWorkbench(AEKey key) {
        return CellWorkbenchFilter.matches(
            itemStack,
            cellItem,
            key,
            partitionInverted,
            partitionFuzzy);
    }

    private void saveChanges() {
        persisted = false;
        if (saveProvider == null) {
            persist();
        } else {
            saveProvider.saveChanges();
        }
    }

    private void notifyStackChange(AEKey key, long amount, boolean listChanged) {
        if (dispatchingListeners) {
            if (currentListener != null) {
                AELog.error(
                    "Universal cell storage listener {} modified the cell during its callback; requesting a full storage refresh.",
                    currentListener.listener);
                currentListener.active = false;
            } else {
                AELog.error(
                    "Universal cell storage was modified during listener dispatch without an active listener; requesting a full storage refresh.");
            }
            listUpdateRequired = true;
            return;
        }

        dispatchingListeners = true;
        int listenerCount = listeners.size();
        try {
            for (int i = 0; i < listenerCount; i++) {
                ListenerRegistration registration = listeners.get(i);
                if (!registration.active) {
                    continue;
                }
                if (!registration.listener.isValid(registration.verificationToken)) {
                    registration.active = false;
                    continue;
                }
                currentListener = registration;
                try {
                    registration.listener.onStackChange(key, amount);
                } finally {
                    currentListener = null;
                }
                if (listUpdateRequired) {
                    break;
                }
            }
        } finally {
            currentListener = null;
            dispatchingListeners = false;
        }
        listUpdateRequired |= listChanged;
        if (listUpdateRequired) {
            notifyListUpdate();
        }
        removeInactiveListeners();
    }

    private void notifyListUpdate() {
        int remainingPasses = listeners.size() + 1;
        dispatchingListeners = true;
        try {
            while (listUpdateRequired && remainingPasses-- > 0) {
                listUpdateRequired = false;
                int listenerCount = listeners.size();
                for (int i = 0; i < listenerCount; i++) {
                    ListenerRegistration registration = listeners.get(i);
                    if (!registration.active) {
                        continue;
                    }
                    if (!registration.listener.isValid(registration.verificationToken)) {
                        registration.active = false;
                        continue;
                    }
                    currentListener = registration;
                    try {
                        registration.listener.onListUpdate();
                    } finally {
                        currentListener = null;
                    }
                }
            }
            if (listUpdateRequired) {
                AELog.error("Universal cell storage listener refresh exceeded its bounded retry count.");
            }
        } finally {
            listUpdateRequired = false;
            currentListener = null;
            dispatchingListeners = false;
        }
    }

    private void removeInactiveListeners() {
        for (int i = listeners.size() - 1; i >= 0; i--) {
            if (!listeners.get(i).active) {
                listeners.remove(i);
            }
        }
    }

    private static final class ListenerRegistration {
        private final MEStorageChangeListener listener;
        private final Object verificationToken;
        private boolean active = true;

        private ListenerRegistration(MEStorageChangeListener listener, Object verificationToken) {
            this.listener = listener;
            this.verificationToken = verificationToken;
        }
    }
}
