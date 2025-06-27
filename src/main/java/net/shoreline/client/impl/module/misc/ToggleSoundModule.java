package net.shoreline.client.impl.module.misc;

import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.manager.world.sound.SoundManager;
import net.shoreline.client.init.Managers;

public class ToggleSoundModule extends ToggleModule {
    private static ToggleSoundModule INSTANCE;

    Config<SoundOption> soundConfig = register(new EnumConfig<>("Sound", "select", SoundOption.NIGHTX, SoundOption.values()));

    public ToggleSoundModule() {
        super("ToggleSound", "toggle Sound", ModuleCategory.MISCELLANEOUS);
        INSTANCE = this;
    }

    public static ToggleSoundModule getInstance() {
        return INSTANCE;
    }

    public SoundOption getSoundOption() {
        return soundConfig.getValue();
    }

    @Override
    protected void onEnable() {
        Managers.SOUND.playSound(
                switch (soundConfig.getValue()) {
                    case NIGHTX -> SoundManager.NIGHTXE;
                    case SIGMA  -> SoundManager.SIGMAE;
                }
        );
    }

    @Override
    protected void onDisable() {
        Managers.SOUND.playSound(
                switch (soundConfig.getValue()) {
                    case NIGHTX -> SoundManager.NIGHTXD;
                    case SIGMA  -> SoundManager.SIGUMAD;
                }
        );
    }

    public enum SoundOption {
        NIGHTX,
        SIGMA
    }
}
