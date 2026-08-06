package com.iamkaf.minisort.forge;

import com.iamkaf.konfig.forge.api.v1.KonfigForgeClientScreens;
import com.iamkaf.minisort.MiniSort;
import com.iamkaf.minisort.MiniSortMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(MiniSort.MOD_ID)
public final class ForgeEntrypoint {
    public ForgeEntrypoint() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            KonfigForgeClientScreens.register(MiniSort.MOD_ID);
        }
        MiniSortMod.init();
    }
}
