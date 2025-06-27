package net.shoreline.client.impl.module.client;

import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.util.Identifier;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.mixin.accessor.AccessorGameOptions;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public final class CapesModule extends ToggleModule
{
    public static CapesModule instance;

    public static final Identifier TEXTURE = Identifier.of("ovaqrebornplus", "cape/cape.png");

    public static final List<String> WHITELIST = new ArrayList<>();

    //
    private final Config<Boolean> userCapeCfg = register(new BooleanConfig("User Cape","show users",true));
    private final Config<Boolean> optifineCfg = register(new BooleanConfig("Optifine","show optifine capes",true));
    //

    private boolean vanillaCapeWasEnabled;

    static {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        new URL("https://ovaqclient.web.fc2.com/capes.txt")
                                .openStream())))
        {
            for (String line; (line = br.readLine()) != null; )
                WHITELIST.add(line.trim());
        } catch (Exception ignored) {}
    }

    public CapesModule()
    {
        super("Capes", "Shows capes", ModuleCategory.CLIENT);
        instance = this;
    }

    @Override
    public void onEnable()
    {
        if (mc.options == null) return;
        vanillaCapeWasEnabled =
                ((AccessorGameOptions) mc.options)
                        .getPlayerModelParts()
                        .contains(PlayerModelPart.CAPE);

        mc.options.togglePlayerModelPart(PlayerModelPart.CAPE, true);
    }

    @Override
    public void onDisable()
    {
        if (mc.options == null) return;
        mc.options.togglePlayerModelPart(PlayerModelPart.CAPE,
                vanillaCapeWasEnabled);
    }

    public boolean showUserCape()      { return userCapeCfg.getValue(); }
    public boolean showOptifineCape()  { return optifineCfg.getValue(); }
}
