package net.shoreline.client.impl.module.misc;

import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.gui.chat.ChatMessageEvent;
import net.shoreline.client.impl.manager.client.HwidManager;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.chat.ChatUtil;
import net.shoreline.eventbus.annotation.EventListener;

public class ChatSuffixModule extends ToggleModule {
    private static final String OVAQ_SUFFIX = " ｜ ᴏᴠᴀǫᴾˡᵘˢ";
    private static final String CATMI_SUFFIX = " ᴄᴀᴛᴍɪ";
    private static final String TEAM_SUFFIX = " ｜ ᴛᴇᴀᴍ 2ᴘ2ꜰᴊᴘ";
    private static final String TEAM_TIKUWA = " ｜ ᴛᴇᴀᴍ ᴛɪᴋᴜᴡᴀ";
    private static final String ZYAGAIMOWARE = " ｜ ᴢʏᴀɢᴀɪᴍᴏᴡᴀʀᴇ\uD835\uDFE2.\uD835\uDFE2.\uD835\uDFE3";
    private static final String DOT_SUFFIX = " ᴅᴏᴛɢᴏᴅ";
    private static final String LEMON = "\u23d0 \u2113\u0454\u043c\u2134\u0e20";

    Config<Mode> modeConfig = register(new EnumConfig<>("Mode", "The suffix mode to append to chat messages", Mode.OVAQ, Mode.values()));
    Config<Boolean> bypassConfig = register(new BooleanConfig("ChatBypass", "Applies bypass formatting to messages", false));

    public ChatSuffixModule() {
        super("ChatSuffix", "Appends Suffix to all sent messages", ModuleCategory.MISCELLANEOUS);
    }

    @EventListener
    public void onChatMessage(ChatMessageEvent.Client event) {
        String originalMessage = event.getMessage();
        String commandPrefix = Managers.COMMAND.getPrefix();

        if (originalMessage.isEmpty() ||  (originalMessage.contains("/") || originalMessage.contains("#") || originalMessage.contains("@")|| originalMessage.startsWith(commandPrefix))) {
            return;
        }

        if (bypassConfig.getValue()) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < originalMessage.length(); i++) {
                builder.append(originalMessage.charAt(i));
                if (i != originalMessage.length() - 1) {
                    builder.append("\"");
                }
            }
            originalMessage = builder.toString();
        }

        String suffix;
        switch (
                modeConfig.getValue()) {
            case CATMI:
                suffix = CATMI_SUFFIX;
                break;
            case TEAM:
                suffix = TEAM_SUFFIX;
                break;
            case TIKUWA:
                suffix = TEAM_TIKUWA;
                break;
            case DOT:
                suffix = DOT_SUFFIX;
                break;
            case LEMON:
                suffix = LEMON;
                break;
            case ZYAGAIMOWARE:
                suffix = ZYAGAIMOWARE;
                break;
            case OVAQ:
            default:
                if (HwidManager.getHWID().equals("39a37031b39137e32e3cd33839c3ea3d534f3613cc3eb3a0")) {//Hypi
                    suffix = " ｜ ᴏᴠᴀǫᴾˡᵘˢ ᴰᵉᵛ";
                } else
                if (HwidManager.getHWID().equals("38c34b3a433f3a632837e36039735a3513e334d37c35737d")) {//Dacho
                    suffix = " ｜ ᴏᴠᴀǫᴾˡᵘˢ ᴇᴀʀʟʏ";
                } else
                if (HwidManager.getHWID().equals("31b3323653b734a3fc37c3353e13463763523983bd3e435d")) {//Tikuwa
                    suffix = " ｜ ᴏᴠᴀǫᴾˡᵘˢ ᴅᴇʙᴜɢ";
                } else
                if (HwidManager.getHWID().equals("3a436d3743e53aa3793bf3043a437e3463273343ca3123c0")) {//WAN
                    suffix = " ｜ ᴬʰᵒᵂᴬᴿᴱ";
                } else{
                    suffix = OVAQ_SUFFIX;
                }
                break;
        }

        String newMessage = originalMessage + suffix;
        ChatUtil.serverSendMessage(newMessage);
        event.cancel();
    }

    public enum Mode {
        OVAQ, CATMI, TEAM, TIKUWA, DOT, LEMON ,ZYAGAIMOWARE
    }
}