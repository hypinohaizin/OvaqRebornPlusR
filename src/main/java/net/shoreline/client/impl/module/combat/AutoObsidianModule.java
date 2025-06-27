package net.shoreline.client.impl.module.combat;

import com.google.common.collect.Lists;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.module.ObsidianPlacerModule;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.client.util.world.ExplosionUtil;
import net.shoreline.eventbus.annotation.EventListener;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author h_ypi
 * @since 1.0
 */
public class AutoObsidianModule extends ObsidianPlacerModule {
    private PlayerEntity target = null;
    private final Map<BlockPos, Animation> fadeList = new HashMap<>();
    private final Map<BlockPos, Long> packets = new HashMap<>();
    private final CacheTimer timer = new CacheTimer();

    Config<Integer> rangeConfig = register(new NumberConfig<>("Range", "Placement range from player", 1, 5, 7));
    Config<Integer> minYConfig = register(new NumberConfig<>("MinY", "Minimum Y offset", 1, 3, 5));
    Config<Float> minDamageConfig = register(new NumberConfig<>("MinDamage", "Min explosion damage", 1.0f, 4.0f, 10.0f));
    Config<Integer> delayConfig = register(new NumberConfig<>("Delay", "Delay between attempts (ticks)", 1, 1, 5));
    Config<Boolean> rotateConfig = register(new BooleanConfig("Rotate", "Rotate to placement", false));
    Config<Boolean> renderConfig = register(new BooleanConfig("Render", "Render place preview", true));
    Config<Integer> fadeTimeConfig = register(new NumberConfig<>("FadeTime", "Fade ms for place preview", 0, 250, 1000, () -> false));

    public AutoObsidianModule() {
        super("AutoObsidian", "Places obsidian at target's feet/around and shows placement fade", ModuleCategory.COMBAT);
    }

    @Override
    public void onEnable() {
        timer.reset();
        fadeList.clear();
        packets.clear();
    }

    @Override
    public void onDisable() {
        fadeList.clear();
        packets.clear();
    }

    @EventListener
    public void onPlayerTick(PlayerTickEvent event) {
        if (mc.player == null || mc.world == null || mc.player.isDead()) return;
        try {
            target = getTargetPlayer();
            if (target == null) return;

            int obsidian = getItemHotbar(Items.OBSIDIAN);
            if (obsidian == -1) return;

            if (timer.passed(delayConfig.getValue())) {
                List<BlockPos> placeList = getOptimalPositions(target, rangeConfig.getValue(), minYConfig.getValue());
                for (BlockPos pos : placeList) {
                    if (pos == null) continue;
                    Long placed = packets.get(pos);
                    if (placed != null && System.currentTimeMillis() - placed < delayConfig.getValue() * 50L) continue;

                    Managers.INTERACT.placeBlock(pos, obsidian, strictDirectionConfig.getValue(), false, (state, angles) -> {
                        if (rotateConfig.getValue()) {
                            if (state) Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
                            else Managers.ROTATION.setRotationSilentSync();
                        }
                    });

                    fadeList.put(pos, new Animation(true, fadeTimeConfig.getValue()));
                    packets.put(pos, System.currentTimeMillis());
                }
                timer.reset();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @EventListener
    public void onRenderWorld(RenderWorldEvent event) {
        if (!renderConfig.getValue() || fadeList.isEmpty()) return;
        RenderBuffers.preRender();
        for (Map.Entry<BlockPos, Animation> entry : new HashSet<>(fadeList.entrySet())) {
            Animation anim = entry.getValue();
            anim.setState(false);
            int boxAlpha = (int) (40 * anim.getFactor());
            int lineAlpha = (int) (100 * anim.getFactor());
            Color boxColor = ColorsModule.getInstance().getColor(boxAlpha);
            Color lineColor = ColorsModule.getInstance().getColor(lineAlpha);
            RenderManager.renderBox(event.getMatrices(), entry.getKey(), boxColor.getRGB());
            RenderManager.renderBoundingBox(event.getMatrices(), entry.getKey(), 1.5f, lineColor.getRGB());
        }
        fadeList.entrySet().removeIf(e -> e.getValue().getFactor() == 0.0);
        RenderBuffers.postRender();
    }

    private List<BlockPos> getOptimalPositions(PlayerEntity target, int range, int minYOffset) {
        Vec3d origin = target.getPos();
        List<BlockPos> sphere = getSphere(origin, range);
        List<BlockPos> result = new ArrayList<>();
        float minDamage = minDamageConfig.getValue();

        for (BlockPos pos : sphere) {
            if (!isAir(pos)) continue;
            if (pos.getY() >= target.getBlockY() - minYOffset) continue;
            double dmg = ExplosionUtil.getDamageTo(target, pos.toCenterPos(), false);
            if (dmg < minDamage) continue;

            result.add(pos);
        }

        // 最もダメージが高い位置を優先
        result.sort((a, b) -> {
            double da = ExplosionUtil.getDamageTo(target, a.toCenterPos(), false);
            double db = ExplosionUtil.getDamageTo(target, b.toCenterPos(), false);
            return Double.compare(db, da);
        });

        return result;
    }

    private List<BlockPos> getSphere(Vec3d origin, int rad) {
        List<BlockPos> sphere = new ArrayList<>();
        for (int x = -rad; x <= rad; ++x)
            for (int y = -rad; y <= rad; ++y)
                for (int z = -rad; z <= rad; ++z) {
                    double dist = x * x + y * y + z * z;
                    if (dist > rad * rad) continue;
                    BlockPos pos = new BlockPos((int) (origin.x + x), (int) (origin.y + y), (int) (origin.z + z));
                    sphere.add(pos);
                }
        return sphere;
    }

    private int getItemHotbar(Item item) {
        for (int i = 0; i < 9; ++i) {
            Item item2 = mc.player.getInventory().getStack(i).getItem();
            if (Item.getRawId(item2) == Item.getRawId(item)) return i;
        }
        return -1;
    }

    private BlockState state(BlockPos p) { return mc.world.getBlockState(p); }
    private boolean isAir(BlockPos p) { return state(p).isAir(); }

    private PlayerEntity getTargetPlayer() {
        List<Entity> entities = Lists.newArrayList(mc.world.getEntities());
        return (PlayerEntity) entities.stream()
                .filter(entity -> entity instanceof PlayerEntity && entity.isAlive() && !mc.player.equals(entity))
                .filter(entity -> mc.player.squaredDistanceTo(entity) <= rangeConfig.getValue() * rangeConfig.getValue())
                .min(Comparator.comparingDouble(entity -> mc.player.squaredDistanceTo(entity)))
                .orElse(null);
    }
}