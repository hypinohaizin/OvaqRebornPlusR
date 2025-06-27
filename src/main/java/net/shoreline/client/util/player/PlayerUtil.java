package net.shoreline.client.util.player;

import net.minecraft.block.BlockState;
import net.minecraft.block.CobwebBlock;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.shoreline.client.util.Globals;
import net.shoreline.client.util.math.position.PositionUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author h_ypi & xgraza
 * @since 1.0
 */
public final class PlayerUtil implements Globals
{
    public static float getLocalPlayerHealth()
    {
        return mc.player.getHealth() + mc.player.getAbsorptionAmount();
    }

    // from MC source
    public static int computeFallDamage(float fallDistance, float damageMultiplier)
    {
        if (mc.player.getType().isIn(EntityTypeTags.FALL_DAMAGE_IMMUNE))
        {
            return 0;
        }
        else
        {
            final StatusEffectInstance statusEffectInstance = mc.player.getStatusEffect(StatusEffects.JUMP_BOOST);
            final float f = statusEffectInstance == null ? 0.0F : (float) (statusEffectInstance.getAmplifier() + 1);
            return MathHelper.ceil((fallDistance - 3.0F - f) * damageMultiplier);
        }
    }

    public static boolean isHolding(final Item item)
    {
        ItemStack itemStack = mc.player.getMainHandStack();
        if (!itemStack.isEmpty() && itemStack.getItem() == item)
        {
            return true;
        }
        itemStack = mc.player.getOffHandStack();
        return !itemStack.isEmpty() && itemStack.getItem() == item;
    }

    public static boolean isHotbarKeysPressed()
    {
        for (KeyBinding binding : mc.options.hotbarKeys)
        {
            if (binding.isPressed())
            {
                return true;
            }
        }
        return false;
    }

    public static boolean inWeb(double expandBb)
    {
        for (BlockPos blockPos : PositionUtil.getAllInBox(mc.player.getBoundingBox().expand(expandBb)))
        {
            BlockState state = mc.world.getBlockState(blockPos);
            if (state.getBlock() instanceof CobwebBlock)
            {
                return true;
            }
        }
        return false;
    }
    public static String getColoredHealth(net.minecraft.entity.player.PlayerEntity p) {
        double hp = p.getHealth() + p.getAbsorptionAmount();
        Formatting color = getHealthColor(p);
        BigDecimal bd = new BigDecimal(hp).setScale(1, RoundingMode.HALF_UP);
        return color + bd.toPlainString();
    }

    public static Formatting getHealthColor(net.minecraft.entity.player.PlayerEntity p) {
        float hp = p.getHealth() + p.getAbsorptionAmount();
        if (hp > 18f)      return Formatting.GREEN;
        else if (hp > 16f) return Formatting.DARK_GREEN;
        else if (hp > 12f) return Formatting.YELLOW;
        else if (hp >  8f) return Formatting.GOLD;
        else if (hp >  4f) return Formatting.RED;
        else                return Formatting.DARK_RED;
    }

    public static String getColoredDistance(net.minecraft.entity.player.PlayerEntity p) {
        double d = mc.player.distanceTo(p);
        Formatting color;
        if      (d <=  5.0) color = Formatting.RED;
        else if (d <= 10.0) color = Formatting.GOLD;
        else if (d <= 15.0) color = Formatting.YELLOW;
        else if (d <= 20.0) color = Formatting.DARK_GREEN;
        else                color = Formatting.GREEN;
        return color.toString();
    }
}
