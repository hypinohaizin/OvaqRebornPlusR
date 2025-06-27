package net.shoreline.client.impl.module.misc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorPlayerInteractEntityC2SPacket;
import net.shoreline.eventbus.annotation.EventListener;

/**
 * @author h_ypi
 */
public class AntiAttackModule extends ToggleModule {
//
    Config<Boolean> friendConfig = register(new BooleanConfig("Friend", "Prevent attacking friends", true));
    Config<Boolean> zoglinConfig = register(new BooleanConfig("Zoglin", "Prevent attacking Zombified Piglin", true));
    Config<Boolean> villagerConfig = register(new BooleanConfig("Villager", "Prevent attacking villagers", false));
    Config<Boolean> oneHpConfig = register(new BooleanConfig("OneHp", "Prevent attacking entity at or below specified HP", false));
    Config<Float> hpConfig = register(new NumberConfig<>("Hp", "Max allowed HP to prevent attack", 1f, 0f, 20f, () -> oneHpConfig.getValue()));

    public AntiAttackModule() {
        super("AntiAttack", "Prevent accidental attacks to friends, specific mobs, and low-HP entities", ModuleCategory.MISCELLANEOUS);
    }

    @EventListener
    public void onPacketOutbound(PacketEvent.Outbound event) {
        if (event.getPacket() instanceof PlayerInteractEntityC2SPacket packet) {
            Entity entity = getEntity(packet);
            if (entity == null) return;

            if (friendConfig.getValue() && Managers.SOCIAL.isFriend(entity.getName().getString())) {
                event.cancel();
                return;
            }
            if (zoglinConfig.getValue() && entity instanceof ZombifiedPiglinEntity) {
                event.cancel();
                return;
            }
            if (villagerConfig.getValue() && entity instanceof VillagerEntity) {
                event.cancel();
                return;
            }
            if (oneHpConfig.getValue() && entity instanceof LivingEntity living) {
                if (living.getHealth() <= hpConfig.getValue()) {
                    event.cancel();
                }
            }
        }
    }

    private Entity getEntity(PlayerInteractEntityC2SPacket packet) {
        if (mc.world == null) return null;
        int id = ((AccessorPlayerInteractEntityC2SPacket) packet).getEntityId();
        return mc.world.getEntityById(id);
    }
}
