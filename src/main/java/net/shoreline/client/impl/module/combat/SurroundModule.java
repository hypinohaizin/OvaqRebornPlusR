package net.shoreline.client.impl.module.combat;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.module.ObsidianPlacerModule;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.impl.module.world.AirPlaceModule;
import net.shoreline.client.impl.module.world.AutoMineModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.math.position.PositionUtil;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.client.util.world.BlastResistantBlocks;
import net.shoreline.eventbus.annotation.EventListener;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author h_ypi
 * @since 1.0
 */
public class SurroundModule extends ObsidianPlacerModule
{
    private static SurroundModule INSTANCE;

    Config<Timing> timingConfig = register(new EnumConfig<>("Timing", "Timing for replacing blocks", Timing.VANILLA, Timing.values()));
    Config<Boolean> prePlaceExplosionConfig = register(new BooleanConfig("PrePlace-Explosions", "Pre places before explosions", false, () -> timingConfig.getValue() == Timing.SEQUENTIAL));
    Config<Boolean> prePlaceTickConfig = register(new BooleanConfig("PrePlace-Tick", "Pre places before ticks", false, () -> timingConfig.getValue() == Timing.SEQUENTIAL));
    Config<Float> placeRangeConfig = register(new NumberConfig<>("PlaceRange", "The placement range for surround", 0.0f, 4.0f, 6.0f));
    Config<Boolean> attackConfig = register(new BooleanConfig("Attack", "Attacks crystals in the way of surround", true));
    Config<Boolean> extendConfig = register(new BooleanConfig("Extend", "Extends surround if the player is not in the center of a block", true));
    Config<Boolean> headConfig = register(new BooleanConfig("CoverHead", "Place a block at your head", false));
    Config<Boolean> mineExtendConfig = register(new BooleanConfig("MineExtend", "Extends surround if the block is being mined", false));
    Config<Boolean> supportConfig = register(new BooleanConfig("Support", "Creates a floor for the surround if there is none", false));
    Config<Integer> shiftTicksConfig = register(new NumberConfig<>("ShiftTicks", "The number of blocks to place per tick", 1, 2, 10));
    Config<Float> shiftDelayConfig = register(new NumberConfig<>("ShiftDelay", "The delay between each block placement interval", 0.0f, 1.0f, 5.0f));
    Config<Boolean> jumpDisableConfig = register(new BooleanConfig("AutoDisable", "Disables after moving out of the hole", true));
    Config<Boolean> renderConfig = register(new BooleanConfig("Render", "Renders where scaffold is placing blocks", false));
    Config<Integer> fadeTimeConfig = register(new NumberConfig<>("Fade-Time", "Time to fade", 0, 250, 1000, () -> false));

    private int blocksPlaced;
    private List<BlockPos> surround = new ArrayList<>();
    private List<BlockPos> placements = new ArrayList<>();
    private final Map<BlockPos, Long> packets = new HashMap<>();
    private final Map<BlockPos, Animation> fadeList = new HashMap<>();
    private double prevY;

    public SurroundModule()
    {
        super("Surround", "Surrounds feet with obsidian", ModuleCategory.COMBAT, 950);
        INSTANCE = this;
    }

    public static SurroundModule getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void onEnable()
    {
        if (mc.player == null)
        {
            return;
        }
        prevY = mc.player.getY();
    }

    @Override
    public void onDisable()
    {
        surround.clear();
        placements.clear();
        packets.clear();
        fadeList.clear();
    }

    @EventListener
    public void onPlayerTick(PlayerTickEvent event)
    {
        blocksPlaced = 0;
        if (SelfTrapModule.getInstance().isEnabled())
        {
            return;
        }

        if (jumpDisableConfig.getValue() && (mc.player.getY() - prevY > 0.5 || mc.player.fallDistance > 1.5f))
        {
            disable();
            return;
        }

        if (!multitaskConfig.getValue() && checkMultitask())
        {
            surround.clear();
            placements.clear();
            return;
        }

        final int slot = getResistantBlockItem();
        if (slot == -1)
        {
            surround.clear();
            placements.clear();
            return;
        }

        surround = getSurround(mc.player);
        if (surround.isEmpty())
        {
            return;
        }

        if (attackConfig.getValue())
        {
            attackBlockingCrystals(surround);
        }
        placements = getPlacementsFromSurround(surround);
        if (placements.isEmpty())
        {
            return;
        }

        if (supportConfig.getValue())
        {
            for (BlockPos block : new ArrayList<>(placements))
            {
                if (block.getY() > mc.player.getBlockY() + 1.0)
                {
                    continue;
                }
                Direction direction = Managers.INTERACT.getInteractDirectionInternal(block, strictDirectionConfig.getValue());
                if (direction == null)
                {
                    placements.add(block.down());
                }
            }
        }
        placements.sort(Comparator.comparingInt(Vec3i::getY));
        while (blocksPlaced < shiftTicksConfig.getValue())
        {
            if (blocksPlaced >= placements.size())
            {
                break;
            }
            BlockPos targetPos = placements.get(blocksPlaced);
            // All rotations for shift ticks must send extra packet
            // This may not work on all servers
            placeBlock(targetPos, slot);
        }

        if (rotateConfig.getValue())
        {
            Managers.ROTATION.setRotationSilentSync();
        }
    }

    @EventListener
    public void onPacketInbound(PacketEvent.Inbound event)
    {
        if (mc.player == null || mc.world == null)
        {
            return;
        }
        if (event.getPacket() instanceof BundleS2CPacket packet)
        {
            for (Packet<?> packet1 : packet.getPackets())
            {
                handlePackets(packet1);
            }
        }
        else
        {
            handlePackets(event.getPacket());
        }
    }

    private void handlePackets(Packet<?> serverPacket)
    {
        if (timingConfig.getValue() != Timing.SEQUENTIAL)
        {
            return;
        }

        if (serverPacket instanceof BlockUpdateS2CPacket packet)
        {
            final BlockState blockState = packet.getState();
            final BlockPos targetPos = packet.getPos();
            if (surround.contains(targetPos))
            {
                if (blockState.isReplaceable() && mc.world.canPlace(DEFAULT_OBSIDIAN_STATE, targetPos, ShapeContext.absent()))
                {
                    final int slot = getResistantBlockItem();
                    if (slot == -1)
                    {
                        return;
                    }
                    placeBlock(targetPos, slot);
                }
                else if (BlastResistantBlocks.isBlastResistant(blockState))
                {
                    packets.remove(targetPos);
                }
            }
        }

        if (blocksPlaced > shiftTicksConfig.getValue() * 2) // Give some leniency if we are getting place on
        {
            return;
        }

        if (serverPacket instanceof ExplosionS2CPacket packet && prePlaceExplosionConfig.getValue())
        {
            BlockPos pos = BlockPos.ofFloored(packet.getX(), packet.getY(), packet.getZ());
            if (surround.contains(pos))
            {
                final int slot = getResistantBlockItem();
                if (slot == -1)
                {
                    return;
                }
                placeBlock(pos, slot);
            }
        }

        if (serverPacket instanceof EntitySpawnS2CPacket packet
                && packet.getEntityType().equals(EntityType.END_CRYSTAL) && prePlaceTickConfig.getValue())
        {
            for (BlockPos pos : surround)
            {
                if (!pos.equals(BlockPos.ofFloored(packet.getX(), packet.getY(), packet.getZ())))
                {
                    continue;
                }

                final int slot = getResistantBlockItem();
                if (slot == -1)
                {
                    return;
                }
                placeBlock(pos, slot);
                break;
            }
        }
    }

    private void placeBlock(BlockPos pos, int slot)
    {
        Managers.INTERACT.placeBlock(pos, slot, strictDirectionConfig.getValue(), false, true, (state, angles) ->
        {
            if (rotateConfig.getValue() && state)
            {
                Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
            }
        });
        packets.put(pos, System.currentTimeMillis());
        blocksPlaced++;
    }

    public void attackBlockingCrystals(List<BlockPos> posList)
    {
        for (BlockPos pos : posList)
        {
            Entity crystalEntity = mc.world.getOtherEntities(null, new Box(pos)).stream()
                    .filter(e -> e instanceof EndCrystalEntity).findFirst().orElse(null);
            if (crystalEntity == null)
            {
                continue;
            }
            Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(crystalEntity, mc.player.isSneaking()));
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            return;
        }
    }

    public List<BlockPos> getPlacementsFromSurround(List<BlockPos> surround)
    {
        List<BlockPos> placements = new ArrayList<>();
        for (BlockPos surroundPos : surround)
        {
            Long placed = packets.get(surroundPos);
            if (shiftDelayConfig.getValue() > 0.0f && placed != null && System.currentTimeMillis() - placed < shiftDelayConfig.getValue() * 50.0f)
            {
                continue;
            }
            if (!mc.world.getBlockState(surroundPos).isReplaceable())
            {
                continue;
            }
            double dist = mc.player.squaredDistanceTo(surroundPos.toCenterPos());
            if (dist > ((NumberConfig) placeRangeConfig).getValueSq())
            {
                continue;
            }

            if (mc.world.canPlace(DEFAULT_OBSIDIAN_STATE, surroundPos, ShapeContext.absent()))
            {
                placements.add(surroundPos);
            }
        }
        return placements;
    }

    public List<BlockPos> getSurround(PlayerEntity player)
    {
        List<BlockPos> surroundBlocks = getSurroundNoDown(player);
        List<BlockPos> playerBlocks = getPlayerBlocks(player);
        for (BlockPos playerPos : playerBlocks)
        {
            if (playerPos.equals(player.getBlockPos()))
            {
                continue;
            }
            surroundBlocks.add(playerPos.down());
        }
        if (mineExtendConfig.getValue())
        {
            for (BlockPos surroundPos : new ArrayList<>(surroundBlocks))
            {
                if (!Managers.BLOCK.isPassed(surroundPos, 0.7f))
                {
                    continue;
                }
                for (Direction direction : Direction.values())
                {
                    if (direction == Direction.DOWN)
                    {
                        continue;
                    }
                    BlockPos blockerPos = surroundPos.offset(direction);
                    if (playerBlocks.contains(blockerPos)
                            || AutoMineModule.getInstance().getMiningBlock() == blockerPos) // Dont want to help our opponent surround
                    {
                        continue;
                    }
                    surroundBlocks.add(blockerPos);
                }
            }
        }

        if (AirPlaceModule.getInstance().isEnabled() && headConfig.getValue())
        {
            surroundBlocks.add(mc.player.getBlockPos().up(2));
        }
        return surroundBlocks;
    }

    public List<BlockPos> getSurroundNoDown(PlayerEntity player)
    {
        return getSurroundNoDown(player, 0.0f);
    }

    public List<BlockPos> getSurroundNoDown(PlayerEntity player, float range)
    {
        List<BlockPos> surroundBlocks = new ArrayList<>();
        List<BlockPos> playerBlocks = getPlayerBlocks(player);
        for (BlockPos pos : playerBlocks)
        {
            if (range > 0.0f && mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()) > range * range)
            {
                continue;
            }
            for (Direction dir : Direction.values())
            {
                if (!dir.getAxis().isHorizontal())
                {
                    continue;
                }
                BlockPos pos1 = pos.offset(dir);
                if (surroundBlocks.contains(pos1) || playerBlocks.contains(pos1))
                {
                    continue;
                }
                surroundBlocks.add(pos1);
            }
        }
        return surroundBlocks;
    }

    public List<BlockPos> getPlayerBlocks(PlayerEntity entity)
    {
        BlockPos playerPos = PositionUtil.getRoundedBlockPos(entity.getX(), entity.getY(), entity.getZ());
        final List<BlockPos> playerBlocks = new ArrayList<>();
        if (extendConfig.getValue())
        {
            playerBlocks.addAll(PositionUtil.getAllInBox(entity.getBoundingBox(), playerPos));
        }
        else
        {
            playerBlocks.add(playerPos);
        }
        return playerBlocks;
    }

    @EventListener
    public void onRenderWorld(RenderWorldEvent event)
    {
        if (SelfTrapModule.getInstance().isEnabled())
        {
            return;
        }
        if (renderConfig.getValue())
        {
            RenderBuffers.preRender();
            for (Map.Entry<BlockPos, Animation> set : fadeList.entrySet())
            {
                set.getValue().setState(false);
                int boxAlpha = (int) (40 * set.getValue().getFactor());
                int lineAlpha = (int) (100 * set.getValue().getFactor());
                Color boxColor = ColorsModule.getInstance().getColor(boxAlpha);
                Color lineColor = ColorsModule.getInstance().getColor(lineAlpha);
                RenderManager.renderBox(event.getMatrices(), set.getKey(), boxColor.getRGB());
                RenderManager.renderBoundingBox(event.getMatrices(), set.getKey(), 1.5f, lineColor.getRGB());
            }
            RenderBuffers.postRender();

            if (placements.isEmpty())
            {
                return;
            }

            for (BlockPos pos : placements)
            {
                Animation animation = new Animation(true, fadeTimeConfig.getValue());
                fadeList.put(pos, animation);
            }
        }

        fadeList.entrySet().removeIf(e ->
                e.getValue().getFactor() == 0.0);
    }

    public boolean isPlacing()
    {
        return !placements.isEmpty();
    }

    public enum Timing
    {
        VANILLA,
        SEQUENTIAL
    }
}
