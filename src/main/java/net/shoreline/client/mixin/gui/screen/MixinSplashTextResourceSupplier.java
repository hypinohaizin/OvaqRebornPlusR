package net.shoreline.client.mixin.gui.screen;

import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.resource.SplashTextResourceSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Random;

@Mixin(SplashTextResourceSupplier.class)
public abstract class MixinSplashTextResourceSupplier {
    @Unique
    private static final Random random = new Random();

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void onGet(CallbackInfoReturnable<SplashTextRenderer> cir) {
        // SplashTextRenderer(String text)
        cir.setReturnValue(new SplashTextRenderer(SPLASHES.get(random.nextInt(SPLASHES.size()))));
    }

    @Unique
    private static final List<String> SPLASHES = List.of(
            "OvaqRebornPlus On Top!",
            "FuckYou",
            "Fuck Minecraft",
            "Ass Hole",
            "自閉症Client",
            "アスペClient",
            "AdhdClient"
    );

}
