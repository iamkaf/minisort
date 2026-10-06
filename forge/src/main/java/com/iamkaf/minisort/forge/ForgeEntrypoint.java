package com.iamkaf.minisort.forge;

import com.iamkaf.konfig.forge.api.v1.KonfigForgeClientScreens;
import com.iamkaf.minisort.MiniSort;
import com.iamkaf.minisort.MiniSortMod;
import com.iamkaf.minisort.client.ClientConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(MiniSort.MOD_ID)
public final class ForgeEntrypoint {
    public ForgeEntrypoint() {
        MiniSortMod.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientConfig.init();
            KonfigForgeClientScreens.register(MiniSort.MOD_ID);
        }
    }
}
