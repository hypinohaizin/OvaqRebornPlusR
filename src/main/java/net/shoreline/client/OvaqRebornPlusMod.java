package net.shoreline.client;

import net.fabricmc.loader.api.FabricLoader;
import net.ovaqrebornplus.client.BuildConfig;

/**
 * @author h_ypi
 * @since 1.0
 */

public class OvaqRebornPlusMod
{
    public static final String MOD_NAME = "OvaqRebornPlus";
    public static final String MOD_VER = BuildConfig.VERSION;
    public static final String MOD_MC_VER = "1.21.1";

    public OvaqRebornPlusMod()
    {
        System.exit(-1);
    }

    /**
     * This code runs as soon as Minecraft is in a mod-load-ready state.
     * However, some things (like resources) may still be uninitialized.
     * Proceed with mild caution.
     */
    public void onInitializeClient()
    {
        OvaqRebornPlus.init();
    }

    public static boolean isBaritonePresent()
    {
        return FabricLoader.getInstance().getModContainer("baritone").isPresent();
    }
}
