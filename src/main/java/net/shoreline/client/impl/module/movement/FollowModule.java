package net.shoreline.client.impl.module.movement;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.shoreline.client.OvaqRebornPlus;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.config.setting.StringConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.eventbus.annotation.EventListener;

import java.util.Comparator;

/**
 * @author h_ypi
 * @since 1.0
 */
public final class FollowModule extends ToggleModule
{
    Config<String> targetnameconfig = register(new StringConfig("Target", "dev", ""));
    Config<Float> speedconfig = register(new NumberConfig<>("Speed", "Follow speed", 1.0f, 0.1f, 10.0f));
    Config<Float> radiusconfig = register(new NumberConfig<>("StopRadius", "Stop distance", 0.0f, 0.0f, 2.0f));

    public FollowModule()
    {
        super("Follow", "Automatically follows a target player", ModuleCategory.MOVEMENT);
    }

    private PlayerEntity findTarget()
    {
        String name = targetnameconfig.getValue().trim();
        if (!name.isEmpty())
        {
            for (PlayerEntity p : mc.world.getPlayers())
            {
                if (p.getName().getString().equalsIgnoreCase(name))
                {
                    return p;
                }
            }
            OvaqRebornPlus.error("Player \"" + name + "\" not found");
            return null;
        }

        return mc.world.getPlayers().stream()
                .filter(p -> !p.equals(mc.player))
                .min(Comparator.comparingDouble(p -> p.squaredDistanceTo(mc.player)))
                .orElse(null);
    }

    @EventListener
    public void onTick(TickEvent e)
    {
        PlayerEntity target = findTarget();
        if (target == null) return;
        followMotion(target);
    }

    private void followMotion(PlayerEntity target) {
        Vec3d cur = mc.player.getPos();
        Vec3d tgt = new Vec3d(target.getX(), target.getY(), target.getZ());
        Vec3d off = tgt.subtract(cur);
        double dist = off.length();

        if (dist <= radiusconfig.getValue())
        {
            mc.player.setVelocity(0, 0, 0);
            return;
        }

        double spd = Math.min(speedconfig.getValue(), dist);
        Vec3d vel = off.normalize().multiply(spd);
        mc.player.setVelocity(vel.x, vel.y, vel.z);
    }
}
