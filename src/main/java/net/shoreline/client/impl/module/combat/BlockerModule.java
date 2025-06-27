package net.shoreline.client.impl.module.combat;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.module.ObsidianPlacerModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.player.RotationUtil;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.eventbus.event.StageEvent;

import java.util.*;
import java.util.stream.Collectors;

public class BlockerModule extends ObsidianPlacerModule {

    Config<TimeMode> timeMode = register(new EnumConfig<>("TimeMode", "When to run blocker", TimeMode.TICK, TimeMode.values()));
    Config<BreakType> breakType = register(new EnumConfig<>("BreakType", "break crystal", BreakType.VANILLA, BreakType.values()));
    Config<BlockMode> fallingMode = register(new EnumConfig<>("FallingMode", "Falling block mode", BlockMode.BREAK, BlockMode.values()));
    Config<BlockPlaceMode> blockPlaced = register(new EnumConfig<>("BlockPlace", "AntiFacePlace block", BlockPlaceMode.STRING, BlockPlaceMode.values()));
    Config<Boolean> anvilBlocker = register(new BooleanConfig("Anvil", "Block falling anvils", true));
    Config<Boolean> fallingBlocker = register(new BooleanConfig("FallingBlocks", "Block all falling blocks", true));
    Config<Boolean> pistonBreaker = register(new BooleanConfig("BreakPiston", "Break pistons in range", true));
    Config<Boolean> pistonBlocker = register(new BooleanConfig("BlockPiston", "Place blocks on pistons", true));
    Config<Boolean> antiFace = register(new BooleanConfig("ShiftAntiFace", "Shift anti-face place", true));
    Config<Boolean> trap = register(new BooleanConfig("Trap", "Trap falling blocks", true));
    Config<Boolean> swing = register(new BooleanConfig("Swing", "Swing hand on block place", false));
    Config<Boolean> rotate = register(new BooleanConfig("Rotate", "Rotate to placement", true));
    //Config<Boolean> packetSwitch = register(new BooleanConfig("PacketSwitch", "Packet switch", true));
    Config<Boolean> packet = register(new BooleanConfig("PacketPlace", "Packet Place", false));
    Config<Integer> tickDelay = register(new NumberConfig<>("TickDelay", "Delay between actions (ticks)", 0, 0, 20));
    Config<Integer> blocksPerTick = register(new NumberConfig<>("BlocksPerTick", "Blocks per tick", 1, 4, 10));
    Config<Double> range = register(new NumberConfig<>("Range", "Piston range", 1.0, 5.0, 10.0));
    Config<Double> yrange = register(new NumberConfig<>("YRange", "Y range", 1.0, 5.0, 10.0));

    private int delayCounter = 0;
    private final List<BlockPos> pistonList = new ArrayList<>();
    private final BlockPos[] sides = new BlockPos[]{new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1)};

    public BlockerModule() {
        super("Blocker", "lemon like blocker", ModuleCategory.COMBAT);
    }

    @Override
    public void onEnable() {
        pistonList.clear();
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (event.getStage() != StageEvent.EventStage.PRE) return;
        var mode = timeMode.getValue();
        if (mode == TimeMode.TICK || mode == TimeMode.BOTH) this.block();
    }

    @EventListener
    public void onUpdate(TickEvent event) {
        if (event.getStage() != StageEvent.EventStage.POST) return;
        var mode = timeMode.getValue();
        if (mode == TimeMode.UPDATE || mode == TimeMode.BOTH) this.block();
    }

    private void block() {
        if (mc.player == null || mc.world == null || mc.player.isDead()) {
            pistonList.clear();
            return;
        }
        if (delayCounter++ < tickDelay.getValue()) return;
        delayCounter = 0;

        if (anvilBlocker.getValue()) blockAnvil();
        if (fallingBlocker.getValue()) blockFallingBlocks();
        if (pistonBreaker.getValue()) blockPiston();
        if (pistonBlocker.getValue()) blockPA();
        if (antiFace.getValue() && mc.options.sneakKey.isPressed()) antiFacePlace();
    }

    private void blockAnvil() {
        BlockPos playerPos = mc.player.getBlockPos();
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof FallingBlockEntity fb && fb.getBlockState().getBlock() instanceof AnvilBlock) {
                if (BlockPos.ofFloored(e.getPos()).equals(playerPos)) {
                    BlockPos head = playerPos.up(2);
                    if (mc.world.getBlockState(head).isAir()) {
                        placeBlockCustom(head, getResistantBlockItem(), rotate.getValue(), packet.getValue(), swing.getValue());
                        return;
                    }
                }
            }
        }
    }

    private void blockFallingBlocks() {
        BlockPos playerPos = mc.player.getBlockPos();
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof FallingBlockEntity fb && !(fb.getBlockState().getBlock() instanceof AnvilBlock)) {
                if (BlockPos.ofFloored(e.getPos()).equals(playerPos)) {
                    // Trap
                    if (trap.getValue()) {
                        placeBlockCustom(playerPos.up(2), getResistantBlockItem(), rotate.getValue(), packet.getValue(), swing.getValue());
                    }
                    int slot = -1;
                    switch (fallingMode.getValue()) {
                        case TORCH -> slot = getBlockItemSlot(Blocks.REDSTONE_TORCH);
                        case SKULL -> slot = -1; // skull slotは未実装
                    }
                    if (slot != -1) {
                        placeBlockCustom(playerPos, slot, rotate.getValue(), packet.getValue(), swing.getValue());
                    } else if (fallingMode.getValue() == BlockMode.BREAK) {
                        mc.interactionManager.attackBlock(playerPos, mc.player.getHorizontalFacing());
                        if (swing.getValue()) mc.player.swingHand(Hand.MAIN_HAND);
                    }
                }
            }
        }
    }
    private void blockPiston() {
        BlockPos playerPos = mc.player.getBlockPos();
        int count = 0;
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof EndCrystalEntity) {
                for (int i = -2; i < 3; i++) {
                    for (int j = -2; j < 3; j++) {
                        if (i == 0 && j == 0) continue;
                        BlockPos bp = playerPos.add(i, 0, j);
                        Block b = mc.world.getBlockState(bp).getBlock();
                        if (b instanceof PistonBlock) {
                            if (breakType.getValue() == BreakType.VANILLA) {
                                mc.interactionManager.attackBlock(bp, mc.player.getHorizontalFacing());
                                if (swing.getValue()) mc.player.swingHand(Hand.MAIN_HAND);
                            }
                            // Packet破壊は未実装
                            if (++count >= blocksPerTick.getValue()) return;
                        }
                    }
                }
            }
        }
    }

    private void blockPA() {
        BlockPos playerPos = mc.player.getBlockPos();
        Set<BlockPos> found = getSphere(playerPos, range.getValue().intValue(), yrange.getValue().intValue())
                .stream().filter(pos -> {
                    Block b = mc.world.getBlockState(pos).getBlock();
                    return b instanceof PistonBlock || b == Blocks.STICKY_PISTON;
                }).collect(Collectors.toSet());
        pistonList.addAll(found);
        pistonList.removeIf(p -> mc.player.squaredDistanceTo(Vec3d.ofCenter(p)) > range.getValue() * range.getValue());
        int slot = getResistantBlockItem();
        int placed = 0;
        if (!pistonList.isEmpty() && slot != -1) {
            for (BlockPos pos : pistonList) {
                BlockPos head = pos.up();
                if ((mc.world.getBlockState(pos).isAir() || mc.world.getBlockState(head).isAir()) && placed < blocksPerTick.getValue()) {
                    placeBlockCustom(pos, slot, rotate.getValue(), packet.getValue(), swing.getValue());
                    placeBlockCustom(head, slot, rotate.getValue(), packet.getValue(), swing.getValue());
                    placed++;
                }
            }
        }
        pistonList.removeIf(p -> mc.world.getBlockState(p).getBlock() == Blocks.OBSIDIAN);
    }

    private void antiFacePlace() {
        BlockPos playerPos = mc.player.getBlockPos();
        int placed = 0;
        for (BlockPos offset : sides) {
            BlockPos pos = playerPos.add(offset);
            Block b = mc.world.getBlockState(pos).getBlock();
            if ((b == Blocks.OBSIDIAN || b == Blocks.BEDROCK) && placed < blocksPerTick.getValue()) {
                int slot;
                if (blockPlaced.getValue() == BlockPlaceMode.STRING) {
                    slot = getSlot(stack -> stack.getItem() == Items.STRING);
                } else {
                    slot = getBlockItemSlot(Blocks.OAK_PRESSURE_PLATE);
                }
                if (slot != -1) {
                    placeBlockCustom(pos.up(), slot, rotate.getValue(), packet.getValue(), swing.getValue());
                    placed++;
                }
            }
        }
    }

    private void placeBlockCustom(BlockPos pos, int slot, boolean rotate, boolean packet, boolean swing) {
        if (slot == -1 || pos == null) return;
        Managers.INVENTORY.setSlot(slot);
        Managers.INTERACT.placeBlock(pos, slot, rotate, packet, (state, angles) -> {
            if (rotate && state) {
                float[] rot = RotationUtil.getRotationsTo(mc.player.getEyePos(), pos.toCenterPos());
                Managers.ROTATION.setRotationSilent(rot[0], rot[1]);
            }
            if (swing) mc.player.swingHand(Hand.MAIN_HAND);
        });
        Managers.INVENTORY.syncToClient();
    }

    private Set<BlockPos> getSphere(BlockPos center, int radius, int yRadius) {
        Set<BlockPos> sphere = new HashSet<>();
        for (int x = -radius; x <= radius; x++)
            for (int y = -yRadius; y <= yRadius; y++)
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radius * radius) continue;
                    sphere.add(center.add(x, y, z));
                }
        return sphere;
    }

    public enum TimeMode {
        TICK,
        UPDATE,
        BOTH,
        FAST
    }
    public enum BreakType {
        VANILLA,
        PACKET
    }
    public enum BlockMode {
        BREAK,
        TORCH,
        SKULL
    }
    public enum BlockPlaceMode {
        PRESSURE,
        STRING
    }

}
