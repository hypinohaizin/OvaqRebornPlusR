package net.shoreline.client.impl.module.combat;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.manager.combat.hole.Hole;
import net.shoreline.client.impl.module.movement.StepModule;
import net.shoreline.client.init.Managers;
import net.shoreline.eventbus.annotation.EventListener;

public class HoleSnapModule extends ToggleModule {
    Config<Float> rangeConfig = register(new NumberConfig<>("Range", "Snap range", 1.0f, 2.5f, 8.0f));
    Config<Double> speedConfig = register(new NumberConfig<>("Speed", "Snap speed", 0.1, 0.1, 1.5));
    Config<Boolean> allowDouble = register(new BooleanConfig("Doubles", "Snap to double holes", false));
    Config<Boolean> stepConfig = register(new BooleanConfig("Step", "Auto Step!", false));

    private Hole targetHole;
    private Vec3d lastTarget;
    private int stuckTicks;
    private boolean wasStepEnabled = false;

    public HoleSnapModule() {
        super("HoleSnap", "Snaps player to the nearest hole", ModuleCategory.COMBAT);
    }

    @Override
    public String getModuleData() {
        if (lastTarget != null && mc.player != null) {
            return String.format("m: %.2f", mc.player.getPos().distanceTo(lastTarget));
        }
        return "";
    }

    @Override
    public void onEnable() {
        stuckTicks = 0;
        lastTarget = null;
        targetHole = findNearestHole();

        if (targetHole == null) {
            disable();
            return;
        }

        if (stepConfig.getValue()) {
            if (StepModule.getINSTANCE() != null && !StepModule.getINSTANCE().isEnabled()) {
                wasStepEnabled = false;
                StepModule.getINSTANCE().enable();
            } else {
                wasStepEnabled = true;
            }
        }
    }

    @Override
    public void onDisable() {
        if (stepConfig.getValue()) {
            if (StepModule.getINSTANCE() != null && !wasStepEnabled && StepModule.getINSTANCE().isEnabled()) {
                StepModule.getINSTANCE().disable();
            }
        }
        targetHole = null;
        stuckTicks = 0;
        lastTarget = null;
    }

    @EventListener
    public void onPlayerTick(PlayerTickEvent event) {
        if (mc.player == null || !mc.player.isAlive()) {
            disable();
            return;
        }
        if (targetHole == null) {
            disable();
            return;
        }

        BlockPos holePos = targetHole.getPos();
        Vec3d playerPos = mc.player.getPos();
        Vec3d targetPos = new Vec3d(holePos.getX() + 0.5, playerPos.y, holePos.getZ() + 0.5);
        lastTarget = targetPos;

        double distance = playerPos.distanceTo(targetPos);
        if (distance < 0.1) {
            disable();
            return;
        }

        double cappedSpeed = Math.min(speedConfig.getValue(), distance);

        double dx = targetPos.x - playerPos.x;
        double dz = targetPos.z - playerPos.z;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length > 0) {
            dx = dx / length * cappedSpeed;
            dz = dz / length * cappedSpeed;
        }

        mc.player.setPosition(playerPos.x + dx, playerPos.y, playerPos.z + dz);

        if (mc.player.horizontalCollision) {
            stuckTicks++;
            if (stuckTicks > 6) {
                disable();
            }
        } else {
            stuckTicks = 0;
        }
    }

    private Hole findNearestHole() {
        Hole nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Hole hole : Managers.HOLE.getHoles()) {
            if (!hole.isStandard() && !(allowDouble.getValue() && hole.isDouble())) continue;
            double dist = hole.squaredDistanceTo(mc.player);
            if (dist < rangeConfig.getValue() * rangeConfig.getValue() && dist < minDist) {
                nearest = hole;
                minDist = dist;
            }
        }
        return nearest;
    }
}
