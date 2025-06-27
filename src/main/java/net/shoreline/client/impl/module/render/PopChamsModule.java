package net.shoreline.client.impl.module.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.Vec3d;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.ColorConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.chams.ChamsModelRenderer;
import net.shoreline.client.api.render.model.StaticBipedEntityModel;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.util.entity.FakePlayerEntity;
import net.shoreline.client.util.render.ColorUtil;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.eventbus.annotation.EventListener;

import java.awt.*;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PopChamsModule extends ToggleModule {

    Config<Integer> fadeTimeConfig = register(new NumberConfig<>("FadeTime", "Totem pop fade ms", 1000, 500, 3000));
    Config<Color> colorConfig = register(new ColorConfig("Color", "PopChams color", new Color(255, 220, 80, 100)));

    private final Map<PopChamEntity, Animation> fadeList = new ConcurrentHashMap<>();

    public PopChamsModule() {
        super("PopChams", "Renders faded model on totem pops", ModuleCategory.RENDER);
    }

    @Override
    public void onDisable() {
        fadeList.clear();
    }

    @EventListener
    public void onPacketInbound(PacketEvent.Inbound event) {
        if (mc.world == null || mc.player == null) return;
        if (event.getPacket() instanceof EntityStatusS2CPacket packet
                && packet.getStatus() == 35) { // EntityStatuses.USE_TOTEM_OF_UNDYING = 35
            Entity entity = packet.getEntity(mc.world);
            if (entity instanceof PlayerEntity player) {
                if (player.getUuid().equals(mc.player.getUuid())) return;
                Animation animation = new Animation(true, fadeTimeConfig.getValue());
                fadeList.put(new PopChamEntity(player, mc.getRenderTickCounter().getTickDelta(true)), animation);
            }
        }
    }

    @EventListener
    public void onRenderWorld(RenderWorldEvent event) {
        if (fadeList.isEmpty()) return;
        RenderBuffers.preRender();
        RenderSystem.disableDepthTest();
        for (Map.Entry<PopChamEntity, Animation> set : new HashSet<>(fadeList.entrySet())) {
            set.getValue().setState(false);
            Color base = colorConfig.getValue();
            int boxAlpha = (int) (base.getAlpha() * set.getValue().getFactor());
            int lineAlpha = (int) (145 * set.getValue().getFactor());
            int boxColor = ColorUtil.withAlpha(base.getRGB(), boxAlpha);
            int lineColor = ColorUtil.withAlpha(base.getRGB(), lineAlpha);
            ChamsModelRenderer.renderStaticPlayerModel(
                    event.getMatrices(),
                    set.getKey(),
                    set.getKey().getModel(),
                    event.getTickDelta(),
                    boxColor, lineColor,
                    1.5f, // 線太さ固定
                    false, true, false);
        }
        fadeList.entrySet().removeIf(e -> e.getValue().getFactor() == 0.0);
        RenderBuffers.postRender();
    }

    public static class PopChamEntity extends FakePlayerEntity {
        private final StaticBipedEntityModel<AbstractClientPlayerEntity> model;
        public PopChamEntity(PlayerEntity player, float tickDelta) {
            super(player);
            this.model = new StaticBipedEntityModel<>((AbstractClientPlayerEntity) player, false, tickDelta);
            this.leaningPitch = player.leaningPitch;
            this.lastLeaningPitch = player.leaningPitch;
            setPose(player.getPose());
        }
        public StaticBipedEntityModel<AbstractClientPlayerEntity> getModel() {
            return model;
        }
    }
}
