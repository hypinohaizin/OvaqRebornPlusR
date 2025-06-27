package net.shoreline.client.mixin.gui.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.AccessibilityOnboardingButtons;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableTextWidget;
import net.minecraft.client.gui.widget.TextIconButtonWidget;
import net.minecraft.client.realms.gui.screen.RealmsNotificationsScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.ovaqrebornplus.client.BuildConfig;
import net.shoreline.client.OvaqRebornPlusMod;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author xgraza
 * @since 03/28/24
 */
@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen
{

    @Shadow
    @Nullable
    private SplashTextRenderer splashText;

    @Shadow
    @Final
    public static Text COPYRIGHT;

    @Shadow
    protected abstract void initWidgetsDemo(int y, int spacingY);

    @Shadow
    protected abstract void initWidgetsNormal(int y, int spacingY);

    @Shadow
    @Nullable
    private RealmsNotificationsScreen realmsNotificationGui;

    @Shadow
    protected abstract boolean isRealmsNotificationsGuiDisplayed();

    @Shadow
    private long backgroundFadeStart;

    @Shadow
    @Final
    private boolean doBackgroundFade;

    public MixinTitleScreen(Text title)
    {
        super(title);
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void hookRender(final DrawContext context, final int mouseX, final int mouseY, final float delta, final CallbackInfo info)
    {
        float f = this.doBackgroundFade ? (float) (Util.getMeasuringTimeMs() - this.backgroundFadeStart) / 1000.0f : 1.0f;
        float g = this.doBackgroundFade ? MathHelper.clamp(f - 1.0f, 0.0f, 1.0f) : 1.0f;
        int i = MathHelper.ceil(g * 255.0f) << 24;
        if ((i & 0xFC000000) == 0)
        {
            return;
        }
        context.drawTextWithShadow(client.textRenderer, String.format("%s %s (%s%s)",
                        OvaqRebornPlusMod.MOD_NAME, OvaqRebornPlusMod.MOD_VER,
                        BuildConfig.BUILD_IDENTIFIER,
                        !BuildConfig.HASH.equals("null") ? "-" + BuildConfig.HASH : ""),
                2, height - (client.textRenderer.fontHeight * 2) - 2, 0xffffff | i);
        String credit = "Created By h_ypi  SpecialThanks zyagaimo tikuwa dacho";
        int creditWidth = client.textRenderer.getWidth(credit);
        context.drawTextWithShadow(client.textRenderer, credit, width - creditWidth - 4, 4, 0xFFFFFF);
    }

    // Autism
    @Inject(method = "init", at = @At(value = "HEAD"), cancellable = true)
    private void hookInit(CallbackInfo ci)
    {
        ci.cancel();
        if (this.splashText == null)
        {
            this.splashText = this.client.getSplashTextLoader().get();
        }
        int i = this.textRenderer.getWidth(COPYRIGHT);
        int j = this.width - i - 2;
        int k = 24;
        int l = this.height / 4 + 48;
        if (this.client.isDemo())
        {
            this.initWidgetsDemo(l, 24);
        }
        else
        {
            this.initWidgetsNormal(l, 24);
        }
        TextIconButtonWidget textIconButtonWidget = this.addDrawableChild(AccessibilityOnboardingButtons.createLanguageButton(20, button -> this.client.setScreen(new LanguageOptionsScreen(this, this.client.options, this.client.getLanguageManager())), true));
        textIconButtonWidget.setPosition(this.width / 2 - 124, l + 72 + 24);
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("menu.options"), button -> this.client.setScreen(new OptionsScreen(this, this.client.options))).dimensions(this.width / 2 - 100, l + 72 + 24, 98, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("menu.quit"), button -> this.client.scheduleStop()).dimensions(this.width / 2 + 2, l + 72 + 24, 98, 20).build());
        TextIconButtonWidget textIconButtonWidget2 = this.addDrawableChild(AccessibilityOnboardingButtons.createAccessibilityButton(20, button -> this.client.setScreen(new AccessibilityOptionsScreen(this, this.client.options)), true));
        textIconButtonWidget2.setPosition(this.width / 2 + 104, l + 72 + 24);
        this.addDrawableChild(new PressableTextWidget(j, this.height - 10, i, 10, COPYRIGHT, button -> this.client.setScreen(new CreditsAndAttributionScreen(this)), this.textRenderer));
        if (this.realmsNotificationGui == null)
        {
            this.realmsNotificationGui = new RealmsNotificationsScreen();
        }
        if (this.isRealmsNotificationsGuiDisplayed())
        {
            this.realmsNotificationGui.init(this.client, this.width, this.height);
        }
    }
}
