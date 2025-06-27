package net.shoreline.client.impl.module.misc;

import net.minecraft.item.Item;
import net.minecraft.item.PickaxeItem;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.ItemListConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;

/**
 * @author h_ypi
 * @since 1.0
 */
public class NoEntityTraceModule extends ToggleModule {
    // Only applies entity bypass when holding a pickaxe
    public final Config<Boolean> pickaxeOnlyConfig =
            register(new BooleanConfig("PickaxeOnly", "Only active with pickaxe in hand", false));
    // List of items to ignore (do NOT bypass entities if holding these)
    public final Config<java.util.List<Item>> ignoredItemConfig = register(new ItemListConfig("IgnoredItems", "Items for which bypass will NOT occur"));

    // Singleton instance for easy static access
    private static NoEntityTraceModule INSTANCE;
    public static NoEntityTraceModule getInstance() {
        return INSTANCE;
    }

    public NoEntityTraceModule() {
        super("NoEntityTrace", "Allows block interaction through entities", ModuleCategory.MISCELLANEOUS);
        INSTANCE = this;
    }

    public boolean shouldIgnore() {
        if (mc.player == null) return false;
        Item hand = mc.player.getMainHandStack().getItem();
        if (pickaxeOnlyConfig.getValue() && hand instanceof PickaxeItem) {
            return true;
        }
        // If item is in ignored list, bypass should NOT happen
        if (((ItemListConfig) ignoredItemConfig).contains(hand)) {
            return false;
        }
        return true;
    }
}
