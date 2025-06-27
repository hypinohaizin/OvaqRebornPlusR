package net.shoreline.client.impl.module.misc;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.eventbus.annotation.EventListener;

public class AutoEatModule extends ToggleModule {

    //
    Config<Boolean> hungerEnableConfig = register(new BooleanConfig("Hunger", "Enable hunger-based auto eat", true));
    Config<Float> hungerConfig = register(new NumberConfig<>("HungerValue", "Eat when hunger at or below this", 1.0f, 19.0f, 20.0f, () -> hungerEnableConfig.getValue()));
    Config<Boolean> healthEnableConfig = register(new BooleanConfig("Health", "Enable health-based auto eat", true));
    Config<Float> healthConfig = register(new NumberConfig<>("HealthValue", "Fuckyou", 5.0f, 15.0f, 36.0f, () -> healthEnableConfig.getValue()));

    private boolean eatingGoldenApple = false;

    public AutoEatModule() {
        super("AutoEat", "Automatically eats when low on hunger or health", ModuleCategory.MISCELLANEOUS);
    }

    @Override
    public void onEnable() {
        eatingGoldenApple = false;
    }

    @Override
    public void onDisable() {
        mc.options.useKey.setPressed(false);
        eatingGoldenApple = false;
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.player.isDead()) return;

        if (healthEnableConfig.getValue()) {
            float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (health <= healthConfig.getValue()) {
                int gapple = getItemHotbar(Items.ENCHANTED_GOLDEN_APPLE);
                if (gapple != -1) {
                    eatingGoldenApple = true;
                    mc.player.getInventory().selectedSlot = gapple;
                    mc.options.useKey.setPressed(true);
                    return;
                }
            }
        }

        if (eatingGoldenApple) {
            if (!mc.player.isUsingItem()) {
                mc.options.useKey.setPressed(false);
                eatingGoldenApple = false;
            }
            return;
        }

        if (hungerEnableConfig.getValue()) {
            HungerManager hungerManager = mc.player.getHungerManager();
            if (hungerManager.getFoodLevel() <= hungerConfig.getValue()) {
                int slot = getFoodSlot();
                if (slot == -1) {
                    mc.options.useKey.setPressed(false);
                    return;
                }
                if (slot == 45) {
                    mc.player.setCurrentHand(Hand.OFF_HAND);
                } else {
                    Managers.INVENTORY.setClientSlot(slot);
                }
                mc.options.useKey.setPressed(true);
            } else {
                mc.options.useKey.setPressed(false);
            }
        }
    }

    private int getItemHotbar(Item item) {
        for (int i = 0; i < 9; ++i) {
            Item stackItem = mc.player.getInventory().getStack(i).getItem();
            if (Item.getRawId(stackItem) == Item.getRawId(item)) {
                return i;
            }
        }
        return -1;
    }

    private int getFoodSlot() {
        int foodLevel = -1;
        int slot = -1;
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.getItem().getComponents().contains(DataComponentTypes.FOOD)) continue;
            if (stack.getItem() == Items.PUFFERFISH || stack.getItem() == Items.CHORUS_FRUIT) continue;
            int hunger = stack.getItem().getComponents().get(DataComponentTypes.FOOD).nutrition();
            if (hunger > foodLevel) {
                slot = i;
                foodLevel = hunger;
            }
        }
        ItemStack offhand = mc.player.getOffHandStack();
        if (offhand.getItem().getComponents().contains(DataComponentTypes.FOOD)) {
            if (offhand.getItem() == Items.PUFFERFISH || offhand.getItem() == Items.CHORUS_FRUIT) {
                return slot;
            }
            int hunger = offhand.getItem().getComponents().get(DataComponentTypes.FOOD).nutrition();
            if (hunger > foodLevel) {
                slot = 45;
            }
        }
        return slot;
    }
}
