package net.shoreline.client.impl.module.misc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.util.chat.ChatUtil;
import net.shoreline.eventbus.annotation.EventListener;

/**
 * @author h_ypi
 * @since 1.0
 */
public class PosLoggerModule extends ToggleModule {
    Config<Integer> delayConfig = register(new NumberConfig<>("Delay", "", 1, 0, 5));
    Config<Boolean> showIDsConfig = register(new BooleanConfig("ShowIDs", "", false));

    private long lastLogTime = 0L;

    public PosLoggerModule() {
        super("PosLogger", "", ModuleCategory.MISCELLANEOUS);
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        PlayerEntity target = getTarget();
        long now = System.currentTimeMillis();

        if (target != null && (now - lastLogTime) > delayConfig.getValue() * 1000L) {
            String direction = target.getMovementDirection().asString(); // east/west等
            String msg = String.format("%s, [%s], X:%.1f, Y:%.1f, Z:%.1f, Yaw:%.1f, Pitch:%.1f",
                    target.getName().getString(), direction,
                    target.getX(), target.getY(), target.getZ(),
                    target.getYaw(), target.getPitch()
            );
            ChatUtil.clientSendMessage(msg);
            lastLogTime = now;
        }

        if (showIDsConfig.getValue()) {
            for (Entity entity : mc.world.getEntities()) {
                entity.setCustomName(Text.literal(String.valueOf(entity.getId())));
                entity.setCustomNameVisible(true);
            }
        }
    }

    private PlayerEntity getTarget() {
        PlayerEntity target = null;
        double minDist = 13.0; // 12以下のみ
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || player.isDead()) continue;
            double dist = mc.player.distanceTo(player);
            if (dist > 12.0) continue;
            if (target == null || dist < mc.player.distanceTo(target)) {
                target = player;
            }
        }
        return target;
    }
}
