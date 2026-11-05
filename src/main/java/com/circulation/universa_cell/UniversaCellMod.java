package com.circulation.universa_cell;

import com.circulation.universa_cell.cells.CellIntegration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION,
    dependencies = "required-after:ae2;" +
        "after:baubles;" +
        "after:megacells")
public class UniversaCellMod {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        CellIntegration.initialize();
    }

}
