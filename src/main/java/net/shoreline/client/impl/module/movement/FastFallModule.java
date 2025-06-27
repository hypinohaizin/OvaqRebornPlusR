package net.shoreline.client.impl.module.movement;

import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

/**
 * @author h_ypi
 * @since 1.0
 */
public class FastFallModule extends ToggleModule
{
    Config<Float> heightConfig = register(new NumberConfig<>("Height", "The maximum height at which instant falling is applied.", 3.0f, 0.5f, 12.0f));
    Config<Boolean> reverseStepConfig = register(new BooleanConfig("ReverseStep", "Enables simple reverse step (Sydney style).", true));

    public FastFallModule() {
        super("FastFall", "Instantly falls down at specified height (ReverseStep).", ModuleCategory.MOVEMENT);
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (!reverseStepConfig.getValue()) return;
        if (event.getStage() != StageEvent.EventStage.PRE) return;
        if (mc.player == null || mc.world == null) return;
        if (mc.player.isRiding() || mc.player.isFallFlying() || mc.player.isHoldingOntoLadder()
                || mc.player.isInLava() || mc.player.isTouchingWater()
                || mc.player.input.jumping || mc.player.input.sneaking) {
            return;
        }
        if (mc.player.isOnGround() && nearBlock(heightConfig.getValue())) {
            mc.player.setVelocity(
                    mc.player.getVelocity().getX(),
                    -heightConfig.getValue(),
                    mc.player.getVelocity().getZ()
            );
        }
    }

    private boolean nearBlock(double height) {
        for (double i = 0; i < height + 0.5; i += 0.01) {
            if (!mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset(0, -i, 0))) {
                return true;
            }
        }
        return false;
    }
}
