package net.shoreline.client.impl.module.misc;

import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.util.chat.ChatUtil;

import java.awt.Desktop;
import java.net.URI;

public class AutoDSModule extends ToggleModule {

    Config<Mode> modeConfig = register(new EnumConfig<>("Mode", "select", Mode.D, Mode.values()));


    public AutoDSModule() {
        super("AutoDS","Open an adult website",ModuleCategory.MISCELLANEOUS);
    }

    @Override
    public void onEnable() {
        String url = null;
        Mode mode = modeConfig.getValue();
        if (mode == Mode.B) {
            url = "https://www.b-hentai.com";
        } else if (mode == Mode.D) {
            url = "https://ddd-smart.net";
        } else if (mode == Mode.H) {
            url = "https://hitomi.la";
        }

        if (url != null) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception e) {
                ChatUtil.clientSendMessage("Unable to open the website: " + e.getMessage());
            }
        }
        disable();
    }

    public enum Mode {
        B,
        D,
        H
    }

}
