package net.shoreline.client.impl.module.movement;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
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

public class BlockMoveModule extends ToggleModule {
    Config<Boolean> middleConfig = register(new BooleanConfig("Middle", "Center in block", true));
    Config<Integer> delayConfig = register(new NumberConfig<>("Delay", "Delay in ms", 250, 0, 2000));
    Config<Boolean> onlyConfig = register(new BooleanConfig("OnlyInBlock", "Only when inside block", true));
    Config<Boolean> avoidConfig = register(new BooleanConfig("AvoidOut", "Avoid when out of block", false, () -> !onlyConfig.getValue()));

    private long lastTime = 0;
    // offsets to check collision around player
    private static final Vec3d[] SIDES = new Vec3d[] {
            new Vec3d(0.24,0,0.24), new Vec3d(-0.24,0,0.24),
            new Vec3d(0.24,0,-0.24), new Vec3d(-0.24,0,-0.24)
    };

    public BlockMoveModule() {
        super("BlockMove", "Walk inside blocks", ModuleCategory.MOVEMENT);
    }

    @Override
    public void onDisable() {
        lastTime = 0;
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (event.getStage() != StageEvent.EventStage.PRE) return;
        var player = mc.player;
        World world = mc.world;
        if (player == null || world == null) return;
        // check inside block
        boolean inBlock = world.getBlockCollisions(player, player.getBoundingBox()).iterator().hasNext();
        if (onlyConfig.getValue() && !inBlock) {
            if (avoidConfig.getValue()) return;
        }
        if (!inBlock) return;
        long now = System.currentTimeMillis();
        if (now - lastTime < delayConfig.getValue()) return;
        lastTime = now;
        // determine base pos
        BlockPos basePos = middleConfig.getValue()
                ? new BlockPos((int) player.getX(), (int) player.getY(), (int) player.getZ())
                : new BlockPos((int) Math.round(player.getX()), (int) player.getY(), (int) Math.round(player.getZ()));
        Direction facing = player.getHorizontalFacing();
        Vec3d newPos = null;
        var input = mc.options;
        if (input.forwardKey.isPressed()) {
            newPos = offsetPos(basePos, facing, true);
        } else if (input.backKey.isPressed()) {
            newPos = offsetPos(basePos, facing.getOpposite(), true);
        } else if (input.leftKey.isPressed()) {
            newPos = offsetPos(basePos, facing.rotateYCounterclockwise(), true);
        } else if (input.rightKey.isPressed()) {
            newPos = offsetPos(basePos, facing.rotateYClockwise(), true);
        }
        if (newPos != null) {
            player.setPosition(newPos.x, newPos.y, newPos.z);
        }
    }

    private Vec3d offsetPos(BlockPos pos, Direction dir, boolean center) {
        BlockPos target = pos.offset(dir);
        Vec3d v = center
                ? new Vec3d(target.getX() + 0.5, target.getY(), target.getZ() + 0.5)
                : new Vec3d(target.getX(), target.getY(), target.getZ());
        return v;
    }
}
