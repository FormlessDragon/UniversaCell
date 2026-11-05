package com.circulation.universa_cell.items;

import ae2.core.definitions.ItemDefinition;
import ae2.items.storage.StorageTier;
import com.circulation.universa_cell.Tags;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;

public final class CellItems {

    public static final ObjectArrayList<ItemDefinition<?>> DEFINITIONS = new ObjectArrayList<>(24);

    @SuppressWarnings("unused")
    public static final ItemDefinition<Item> CONVERGENT_CRYSTAL = define("convergent_crystal", new Item());
    public static final ItemDefinition<Item> UNIVERSAL_CELL_HOUSING = define("universal_cell_housing", new Item());

    public static final ItemDefinition<UniversalStorageCellItem> UNIVERSAL_CELL_1K =
        define("universal_cell_1k", new UniversalStorageCellItem(StorageTier.SIZE_1K));
    public static final ItemDefinition<UniversalStorageCellItem> UNIVERSAL_CELL_4K =
        define("universal_cell_4k", new UniversalStorageCellItem(StorageTier.SIZE_4K));
    public static final ItemDefinition<UniversalStorageCellItem> UNIVERSAL_CELL_16K =
        define("universal_cell_16k", new UniversalStorageCellItem(StorageTier.SIZE_16K));
    public static final ItemDefinition<UniversalStorageCellItem> UNIVERSAL_CELL_64K =
        define("universal_cell_64k", new UniversalStorageCellItem(StorageTier.SIZE_64K));
    public static final ItemDefinition<UniversalStorageCellItem> UNIVERSAL_CELL_256K =
        define("universal_cell_256k", new UniversalStorageCellItem(StorageTier.SIZE_256K));
    public static final ObjectArrayList<ItemDefinition<UniversalStorageCellItem>> UNIVERSAL_CELLS = ObjectArrayList.of(
        UNIVERSAL_CELL_1K, UNIVERSAL_CELL_4K, UNIVERSAL_CELL_16K, UNIVERSAL_CELL_64K, UNIVERSAL_CELL_256K);
    public static final ItemDefinition<PortableUniversalStorageCellItem> PORTABLE_UNIVERSAL_CELL_1K =
        define("portable_universal_cell_1k", new PortableUniversalStorageCellItem(StorageTier.SIZE_1K));
    public static final ItemDefinition<PortableUniversalStorageCellItem> PORTABLE_UNIVERSAL_CELL_4K =
        define("portable_universal_cell_4k", new PortableUniversalStorageCellItem(StorageTier.SIZE_4K));
    public static final ItemDefinition<PortableUniversalStorageCellItem> PORTABLE_UNIVERSAL_CELL_16K =
        define("portable_universal_cell_16k", new PortableUniversalStorageCellItem(StorageTier.SIZE_16K));
    public static final ItemDefinition<PortableUniversalStorageCellItem> PORTABLE_UNIVERSAL_CELL_64K =
        define("portable_universal_cell_64k", new PortableUniversalStorageCellItem(StorageTier.SIZE_64K));
    public static final ItemDefinition<PortableUniversalStorageCellItem> PORTABLE_UNIVERSAL_CELL_256K =
        define("portable_universal_cell_256k", new PortableUniversalStorageCellItem(StorageTier.SIZE_256K));
    public static final ObjectArrayList<ItemDefinition<? extends PortableUniversalStorageCellItem>> PORTABLE_UNIVERSAL_CELLS = ObjectArrayList.of(
        PORTABLE_UNIVERSAL_CELL_1K,
        PORTABLE_UNIVERSAL_CELL_4K,
        PORTABLE_UNIVERSAL_CELL_16K,
        PORTABLE_UNIVERSAL_CELL_64K,
        PORTABLE_UNIVERSAL_CELL_256K);

    private CellItems() {
    }

    public static void register(RegistryEvent.Register<Item> event) {
        if (Loader.isModLoaded("megacells")) {
            MEGACellItems.register();
        }
        for (ItemDefinition<?> definition : DEFINITIONS) {
            event.getRegistry().register(definition.item());
        }
    }

    public static ObjectArrayList<Item> allItems() {
        ObjectArrayList<Item> items = new ObjectArrayList<>(DEFINITIONS.size());
        for (ItemDefinition<?> definition : DEFINITIONS) {
            items.add(definition.item());
        }
        return items;
    }

    public static void registerOptionalUpgrades() {
        if (Loader.isModLoaded("megacells")) {
            MEGACellItems.registerUpgrades();
        }
    }

    static <T extends Item> ItemDefinition<T> define(String name, T item) {
        ItemDefinition<T> definition = new ItemDefinition<>(
            new ResourceLocation(Tags.MOD_ID, name),
            item,
            UniversalCellCreativeTab.TAB);
        DEFINITIONS.add(definition);
        return definition;
    }
}
