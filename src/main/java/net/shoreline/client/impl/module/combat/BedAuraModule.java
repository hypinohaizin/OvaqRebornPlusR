package net.shoreline.client.impl.module.combat;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.module.BlockPlacerModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.client.util.player.RotationUtil;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

import java.util.ArrayList;
import java.util.List;

public class BedAuraModule extends BlockPlacerModule {
    Config<Float> targetRangeConfig = register(new NumberConfig<>("EnemyRange", "Range to search for enemies", 1.0f, 10.0f, 13.0f));
    Config<Float> placeRangeConfig = register(new NumberConfig<>("PlaceRange", "Bed place range", 0.1f, 4.0f, 6.0f));
    Config<Float> placeSpeedConfig = register(new NumberConfig<>("PlaceSpeed", "Speed to place beds", 0.1f, 18.0f, 20.0f));
    //Config<Boolean> swingConfig = register(new BooleanConfig("Swing", "Swing hand on explode", true));
    Config<SwapMode> swapConfig = register(new EnumConfig<>("Swap", "Swap mode for hotbar switching", SwapMode.NORMAL, SwapMode.values()));
    Config<Boolean> refillConfig = register(new BooleanConfig("RefillBeds", "Auto refill beds from inventory", true));
    Config<Integer> minBedCountConfig = register(new NumberConfig<>("MinBedCount", "Min beds in hotbar before refill", 1, 1, 4));

    private final Timer placeTimer = new CacheTimer();

    public BedAuraModule() {
        super("BedAura", "Auto places and explodes beds near enemies", ModuleCategory.COMBAT, 741);
    }

    private BlockPos lastPlaced = null;

    @EventListener
    public void onPlayerTick(TickEvent event) {
        if (event.getStage() != StageEvent.EventStage.PRE) return;
        if (refillConfig.getValue()) refillBeds();

        int bedSlot = getBedSlot();
        if (bedSlot == -1) return;

        BlockPos targetPos = findPlacePos();
        if (targetPos != null && placeTimer.passed(1000.0f - placeSpeedConfig.getValue() * 50.0f)) {
            if (rotateConfig != null && rotateConfig.getValue()) {
                float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePos(), targetPos.toCenterPos());
                setRotation(rotations[0], rotations[1]);
            }
            swapToBedSlot(bedSlot);
            placeBed(targetPos);  // 足側のみ渡す
            placeTimer.reset();
        }

        for (BlockPos pos : getNearbyBeds()) {
            explodeBed(pos);
        }
    }

    private void explodeBed(BlockPos pos) {
        BlockHitResult result = new BlockHitResult(pos.toCenterPos(), Direction.UP, pos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, result);
        Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }


    private List<BlockPos> getNearbyBeds() {
        List<BlockPos> beds = new ArrayList<>();
        BlockPos playerPos = mc.player.getBlockPos();
        int radius = 6;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock() instanceof net.minecraft.block.BedBlock) {
                        beds.add(pos);
                    }
                }
            }
        }
        return beds;
    }


    private void refillBeds() {
        int bedCount = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BedItem) {
                bedCount += stack.getCount();
            }
        }
        if (bedCount >= minBedCountConfig.getValue()) return;

        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BedItem) {
                for (int j = 0; j < 9; j++) {
                    ItemStack hotbarStack = mc.player.getInventory().getStack(j);
                    if (hotbarStack.isEmpty()) {
                        Managers.INVENTORY.click(i, 0, SlotActionType.SWAP);
                        return;
                    }
                }
            }
        }
    }

    private BlockPos findPlacePos() {
        List<BlockPos> sphere = getSphere(mc.player.getPos(), Math.min(placeRangeConfig.getValue(), 5.0f));
        for (BlockPos pos : sphere) {
            if (canPlaceBedAt(pos) && isEnemyNear(pos, targetRangeConfig.getValue())) {
                return pos;
            }
        }
        return null;
    }

    private boolean placeBed(BlockPos footPos) {
        for (var dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            BlockPos headPos = footPos.offset(dir);
            if (!isAirOrReplaceable(headPos)) continue;
            if (!mc.world.getBlockState(headPos.down()).isSolidBlock(mc.world, headPos.down())) continue;
            if (mc.player.squaredDistanceTo(footPos.toCenterPos()) > 25) continue;
            BlockHitResult result = new BlockHitResult(footPos.toCenterPos(), dir, footPos, false);

            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, result);
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            Managers.INVENTORY.syncToClient();
            return true;
        }
        return false;
    }


    private boolean canPlaceBedAt(BlockPos pos) {
        for (var dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            BlockPos headPos = pos.offset(dir);
            if (isAirOrReplaceable(pos) && isAirOrReplaceable(headPos)
                    && mc.world.getBlockState(pos.down()).isSolidBlock(mc.world, pos.down())
                    && mc.world.getBlockState(headPos.down()).isSolidBlock(mc.world, headPos.down())) {
                return true;
            }
        }
        return false;
    }

    private boolean isAirOrReplaceable(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return state.isReplaceable() || state.isAir();
    }


    private boolean isEnemyNear(BlockPos pos, float range) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || !(entity instanceof PlayerEntity) || Managers.SOCIAL.isFriend(entity.getName())) continue;
            if (entity.squaredDistanceTo(pos.toCenterPos()) < range * range) {
                return true;
            }
        }
        return false;
    }

    private List<BlockPos> getSphere(Vec3d origin, float range) {
        List<BlockPos> sphere = new ArrayList<>();
        double rad = Math.ceil(range);
        for (double x = -rad; x <= rad; ++x)
            for (double y = -rad; y <= rad; ++y)
                for (double z = -rad; z <= rad; ++z)
                    sphere.add(new BlockPos((int) (origin.x + x), (int) (origin.y + y), (int) (origin.z + z)));
        return sphere;
    }

    private int getBedSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BedItem) return i;
        }
        return -1;
    }

    private void swapToBedSlot(int bedSlot) {
        switch (swapConfig.getValue()) {
            case NORMAL -> Managers.INVENTORY.setSlot(bedSlot);
            case SILENT -> Managers.INVENTORY.setSlotForced(bedSlot);
            case OFF -> {}
        }
    }

    public enum SwapMode {
        NORMAL,
        SILENT,
        OFF
    }
}
