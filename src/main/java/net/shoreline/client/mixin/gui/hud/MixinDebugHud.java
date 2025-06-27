package net.shoreline.client.mixin.gui.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.ovaqrebornplus.client.BuildConfig;
import net.shoreline.client.OvaqRebornPlusMod;
import net.shoreline.client.impl.module.render.SmartF3Module;
import net.shoreline.client.util.string.StringUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;

/**
 * @author hockeyl8
 * @since 1.0
 */
@Mixin(DebugHud.class)
public abstract class MixinDebugHud
{
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    protected abstract List<String> getLeftText();

    @Redirect(method = "drawLeftText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/DebugHud;getLeftText()Ljava/util/List;"))
    private List<String> redirectRightTextEarly(DebugHud instance)
    {
        List<String> list = getLeftText();
        String modName = String.format("%s %s (%s%s%s)",
                OvaqRebornPlusMod.MOD_NAME, OvaqRebornPlusMod.MOD_VER, BuildConfig
                        .BUILD_IDENTIFIER, "-",
                !BuildConfig.HASH.equals("null") ? "-" + BuildConfig.HASH : "");

        list.add(1, modName);
        return list;
    }
    @Inject(method = "drawText", at = @At("HEAD"))
    private void modifyDebugText(DrawContext context, List<String> text, boolean left, CallbackInfo ci) {
        if (SmartF3Module.getInstance().isEnabled()) {
            text.removeIf(Objects::isNull);
            if (SmartF3Module.getInstance().getActiveRenderer()) {
                text.removeIf(s -> s.startsWith("[Fabric] Active renderer:"));
            }

            if (SmartF3Module.getInstance().getIris()) {
                text.removeIf(s -> s.startsWith("[Iris]"));
                text.removeIf(s -> s.startsWith("[Entity Batching]"));
            }

            if (SmartF3Module.getInstance().getSodium()) {
                var sodiumIndex = StringUtil.indexOfStartingWith(text, "§aSodium Renderer");
                if (sodiumIndex != -1) {
                    text.subList(sodiumIndex, Math.min(sodiumIndex + 7, text.size())).clear();

                    if (sodiumIndex > 0 && text.get(sodiumIndex - 1).isEmpty()) {
                        text.remove(sodiumIndex - 1);
                    }
                }
            }
            if (SmartF3Module.getInstance().getModernFix()) {
                var modernFixIndex = StringUtil.indexOfStartingWith(text, "ModernFix");

                if (modernFixIndex != -1) {
                    text.subList(modernFixIndex, Math.min(modernFixIndex + 2, text.size())).clear();
                }
            }
            while (!text.isEmpty() && text.get(0).isEmpty()) {
                text.remove(0);
            }
        }
    }

    @Redirect(method = "getRightText", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/hit/HitResult;getType()Lnet/minecraft/util/hit/HitResult$Type;", ordinal = 1))
    protected final HitResult.Type changeFluidHitType(HitResult result) {
        if (SmartF3Module.getInstance().isEnabled()) {
            if (SmartF3Module.getInstance().getShyFluids() && result instanceof BlockHitResult blockHitResult && client.world.getFluidState(blockHitResult.getBlockPos()).isEmpty()) {
                return HitResult.Type.MISS;
            }
        }
        return result.getType();
    }
}
