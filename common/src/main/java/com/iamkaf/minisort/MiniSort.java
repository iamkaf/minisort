package com.iamkaf.minisort;

//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MiniSort {
    public static final String MOD_ID = "minisort";
    public static final String MOD_NAME = "Minisort";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    private MiniSort() {
    }

//? if >=1.21.11 {
    public static Identifier resource(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
//?} else {
    public static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
//?}
}
