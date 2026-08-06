package com.iamkaf.minisort.neoforge;

import com.iamkaf.konfig.neoforge.api.v1.KonfigNeoForgeClientScreens;
import com.iamkaf.minisort.MiniSort;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = MiniSort.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeClientEntrypoint {
    public NeoForgeClientEntrypoint(ModContainer container) {
        KonfigNeoForgeClientScreens.register(container, MiniSort.MOD_ID);
    }
}
