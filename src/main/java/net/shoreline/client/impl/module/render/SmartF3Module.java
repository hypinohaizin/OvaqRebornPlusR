package net.shoreline.client.impl.module.render;

import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;

/**
 * @author h_ypi
 * @since 1.0
 */
public class SmartF3Module extends ToggleModule {
    private static SmartF3Module INSTANCE;

    private final Config<Boolean> hideActiveRendererConfig = register(new BooleanConfig("Hide Active Renderer", "minecraft", true));
    private final Config<Boolean> shyFluidsConfig = register(new BooleanConfig("Hide ShyFluids", "minecraft", true));
    private final Config<Boolean> sodiumConfig = register(new BooleanConfig("Hide Sodium", "Hide Sodium Mod info", true));
    private final Config<Boolean> irisConfig = register(new BooleanConfig("Hide Iris", "Hide Iris Mod info", true));
    private final Config<Boolean> modernFixConfig = register(new BooleanConfig("Hide ModernFix", "Hide ModernFix Mod info", true));

    public SmartF3Module() {
        super("SmartF3", "Clean debug screen", ModuleCategory.RENDER);
        INSTANCE = this;
    }
    public static SmartF3Module getInstance()
    {
        return INSTANCE;
    }

    public boolean getActiveRenderer() {
        return hideActiveRendererConfig.getValue();
    }

    public boolean getShyFluids() {
        return shyFluidsConfig.getValue();
    }

    public boolean getSodium() {
        return sodiumConfig.getValue();
    }

    public boolean getIris() {
        return irisConfig.getValue();
    }

    public boolean getModernFix() {
        return modernFixConfig.getValue();
    }
}
