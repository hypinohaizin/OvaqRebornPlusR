package net.shoreline.client.impl.module.movement;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.manager.pathing.PathManagers;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class AutoWalkModule extends ToggleModule
{
    private static AutoWalkModule INSTANCE;

    Config<Mode> modeConfig = register(new EnumConfig<>("Mode", "AutoWalk mode", Mode.NORMAL, Mode.values()));
    Config<Boolean> lockConfig = register(new BooleanConfig("Lock", "Stops movement when sneaking or jumping", false));
    Config<Integer> walkLengthConfig = register(new NumberConfig<>("WalkLength", "How far to walk forward (blocks)", 50, 5, 500));

    public AutoWalkModule()
    {
        super("AutoWalk", "Automatically moves forward", ModuleCategory.MOVEMENT);
        INSTANCE = this;
    }

    public static AutoWalkModule getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void onDisable()
    {
        mc.options.forwardKey.setPressed(false);
        if (modeConfig.getValue() == Mode.BARITONE) {
            PathManagers.get().cancel();
        }
    }

    @EventListener
    public void onTick(TickEvent event)
    {
        if (event.getStage() != StageEvent.EventStage.PRE) return;

        if (modeConfig.getValue() == Mode.BARITONE) {
            if (mc.player != null) {
                Direction facing = mc.player.getHorizontalFacing();
                BlockPos origin = mc.player.getBlockPos();
                int length = walkLengthConfig.getValue();
                BlockPos goal = origin.offset(facing, length);
                if (!PathManagers.get().isPathing()) {
                    PathManagers.get().setGoal(goal);
                }
            }
            return;
        }

        mc.options.forwardKey.setPressed(!mc.options.sneakKey.isPressed()
                && (!lockConfig.getValue() || (!mc.options.jumpKey.isPressed() && mc.player.isOnGround())));
    }

    public enum Mode {
        NORMAL,
        BARITONE
    }
}
