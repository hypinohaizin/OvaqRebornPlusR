package net.shoreline.client.impl.manager.pathing;

import net.minecraft.util.math.BlockPos;

/**
 * @author h_ypi
 * @since 1.0
 */

public interface IPathManager {
    boolean isPathing();
    void setGoal(BlockPos pos);
    void cancel();
}
