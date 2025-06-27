package net.shoreline.client.impl.module.world;

import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.shoreline.client.OvaqRebornPlusMod;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.manager.player.rotation.Rotation;
import net.shoreline.client.impl.manager.pathing.PathManagers;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.chat.ChatUtil;
import net.shoreline.client.util.player.RotationUtil;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class AutoTunnelModule extends ToggleModule {

    Config<Mode> modeConfig = register(new EnumConfig<>("Mode", "Tunnel Mode", Mode.NORMAL, Mode.values()));
    Config<Integer> lengthConfig = register(new NumberConfig<>("Length", "Tunnel length (in blocks)", 1, 1, 10));
    Config<Boolean> rotateConfig = register(new BooleanConfig("Rotate", "Rotate to mining direction", true));

    private BlockPos startPos = null;
    private Direction startFacing = null;
    private int startY = -1;
    private BlockPos currentGoal = null;
    private boolean baritoneStarted = false;

    public AutoTunnelModule() {
        super("AutoTunnel", "Automatically mines a tunnel in a straight line", ModuleCategory.WORLD);
    }

    @Override
    public void onEnable() {
        baritoneStarted = false;
        if (mc.player != null) {
            startPos = mc.player.getBlockPos();
            startFacing = mc.player.getHorizontalFacing();
            startY = startPos.getY();
            currentGoal = null;
        }
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (event.getStage() != StageEvent.EventStage.PRE) return;
        var player = mc.player;
        var world = mc.world;
        if (player == null || world == null) return;

        if (modeConfig.getValue() == Mode.NORMAL) {
            baritoneStarted = false;
            Direction facing = player.getHorizontalFacing();
            BlockPos origin = player.getBlockPos();
            for (int i = 1; i <= lengthConfig.getValue(); i++) {
                BlockPos pos = origin.offset(facing, i);
                var state1 = world.getBlockState(pos);
                if (!state1.isAir() && state1.getFluidState().isEmpty()) {
                    if (rotateConfig.getValue()) {
                        float[] yawPitch = RotationUtil.getRotationsTo(player.getEyePos(), pos.toCenterPos());
                        Managers.ROTATION.setRotation(new Rotation(0, yawPitch[0], yawPitch[1]));
                    }
                    mc.interactionManager.attackBlock(pos, facing);
                    player.swingHand(Hand.MAIN_HAND);
                    break;
                }
                BlockPos posUp = pos.up();
                var state2 = world.getBlockState(posUp);
                if (!state2.isAir() && state2.getFluidState().isEmpty()) {
                    if (rotateConfig.getValue()) {
                        float[] yawPitch = RotationUtil.getRotationsTo(player.getEyePos(), posUp.toCenterPos());
                        Managers.ROTATION.setRotation(new Rotation(0, yawPitch[0], yawPitch[1]));
                    }
                    mc.interactionManager.attackBlock(posUp, facing);
                    player.swingHand(Hand.MAIN_HAND);
                    break;
                }
            }
        } else if (modeConfig.getValue() == Mode.BARITONE) {
            if (!OvaqRebornPlusMod.isBaritonePresent()) {
                if (!baritoneStarted) {
                    ChatUtil.clientSendMessage("§s [AutoTunnel] §7Baritone is not present!");
                    baritoneStarted = true;
                }
                return;
            }
            if (startPos == null || startFacing == null || startY == -1) return;

            if (!PathManagers.get().isPathing()) {
                if (currentGoal == null) {
                    currentGoal = new BlockPos(
                            startPos.offset(startFacing, lengthConfig.getValue()).getX(),
                            startY,
                            startPos.offset(startFacing, lengthConfig.getValue()).getZ()
                    );
                } else {
                    currentGoal = new BlockPos(
                            currentGoal.offset(startFacing, lengthConfig.getValue()).getX(),
                            startY,
                            currentGoal.offset(startFacing, lengthConfig.getValue()).getZ()
                    );
                }
                PathManagers.get().setGoal(currentGoal);
                ChatUtil.clientSendMessage("§s [AutoTunnel] §7 Pathing to: " + currentGoal.toShortString());
                baritoneStarted = true;
            }
        }
    }

    @Override
    public void onDisable() {
        baritoneStarted = false;
        if (modeConfig.getValue() == Mode.BARITONE) {
            PathManagers.get().cancel();
        }
        startPos = null;
        startFacing = null;
        startY = -1;
        currentGoal = null;
    }

    public enum Mode {
        NORMAL,
        BARITONE
    }
}
