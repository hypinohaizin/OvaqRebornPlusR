package net.shoreline.client.impl.manager.pathing;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import net.minecraft.util.math.BlockPos;

/**
 * @author h_ypi
 * @since 1.0
 */

public class BaritonePathManager implements IPathManager {

    @Override
    public boolean isPathing() {
        try {
            return BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void setGoal(BlockPos pos) {
        try {
            BaritoneAPI.getProvider().getPrimaryBaritone()
                    .getCustomGoalProcess()
                    .setGoalAndPath(new GoalBlock(pos.getX(), pos.getY(), pos.getZ()));
        } catch (Throwable t) {
        }
    }

    @Override
    public void cancel() {
        try {
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().forceCancel();
        } catch (Throwable t) {
        }
    }
}
