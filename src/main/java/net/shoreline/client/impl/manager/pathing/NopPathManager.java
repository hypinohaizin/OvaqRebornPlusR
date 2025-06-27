package net.shoreline.client.impl.manager.pathing;

import net.minecraft.util.math.BlockPos;

/**
 * @author h_ypi
 * @since 1.0
 */

public class NopPathManager implements IPathManager {
    @Override public boolean isPathing() { return false; }
    @Override public void setGoal(BlockPos pos) { }
    @Override public void cancel() { }
}
