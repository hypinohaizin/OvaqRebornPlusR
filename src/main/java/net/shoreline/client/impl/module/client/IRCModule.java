package net.shoreline.client.impl.module.client;

import net.minecraft.util.Formatting;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.gui.chat.ChatMessageEvent;
import net.shoreline.client.impl.event.network.SocketReceivedPacketEvent;
import net.shoreline.client.socket.SocketChat;
import net.shoreline.client.socket.SocketWebhookManager;
import net.shoreline.client.socket.exception.SocketNickErrorException;
import net.shoreline.client.util.chat.ChatUtil;
import net.shoreline.eventbus.annotation.EventListener;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IRCModule extends ToggleModule {
    private static IRCModule INSTANCE;
    public static SocketChat chat;

    Config<Boolean> discordConfig = register(new BooleanConfig("Discord", "Send to Discord via webhook.", true));
    Config<Boolean> receiveDiscordConfig = register(new BooleanConfig("ReceiveDiscordMessage", "Show Discord messages.", true));

    private final String[] devs = {
            "h_ypi"
    };
    private final String[] vips = {
            "tikuwa",
            "dacho",
            "wanwanfan"
    };

    private final Map<String, String> mentionMap = Stream.of(new String[][] {
            {"@hypi", "<@1143899002463588423>"},
            {"@tikuwa", "<@1344432842578329653>"},
            {"@dacho", "<@1113048267136176169>"},
            {"@wan", "<@1094189134194679818>"}
    }).collect(Collectors.toMap(a -> a[0], a -> a[1]));

    public IRCModule() {
        super("IRC", "global chat. prefix: @", ModuleCategory.CLIENT);
        INSTANCE = this;
    }

    public static IRCModule getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        try {
            if (chat == null) {
                chat = new SocketChat("wss://hack.chat/chat-ws", "oovahqdiushnauidad", mc.getSession().getUsername(), "");
            }
            chat.connect();
        } catch (SocketNickErrorException e) {
            ChatUtil.error("Invalid IRC nick: " + e.getMessage());
            disable();
            return;
        }
        ChatUtil.clientSendMessage("IRC Connected");
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (chat != null) chat.disconnect();
        ChatUtil.clientSendMessage("IRC Disconnected");
    }

    private String replaceMentions(String content) {
        for (var e : mentionMap.entrySet()) {
            content = content.replace(e.getKey(), e.getValue());
        }
        return content;
    }

    @EventListener
    public void onChatMessage(ChatMessageEvent.Client event) {
        String msg = event.getMessage();
        if (!msg.startsWith("@")) return;
        event.cancel();

        String trimmed = msg.substring(1);
        if (trimmed.length() > 150) {
            ChatUtil.error("You cannot send more than 150 characters.");
            return;
        }

        ChatUtil.clientSendMessageRaw(Formatting.GRAY + "[" + Formatting.AQUA + "IRC" + Formatting.GRAY + "] " + Formatting.WHITE + mc.getSession().getUsername() + ": " + trimmed);

        new Thread(() -> {
            if (discordConfig.getValue()) {
                try {
                    SocketWebhookManager.send(mc.getSession().getUsername(), replaceMentions(trimmed));
                } catch (Exception ex) {
                    ChatUtil.error("Discord notification error: " + ex.getMessage());
                }
            }

            try {
                if (chat == null) {
                    chat = new SocketChat("wss://hack.chat/chat-ws", "oovahqdiushnauidad", mc.getSession().getUsername(), "");
                }
                if (!chat.isConnected()) {
                    chat.connect();
                }
                chat.send(trimmed);
            } catch (Exception ex) {
                ChatUtil.error("IRC send error: " + ex.getMessage());
            }
        }).start();
    }

    @EventListener
    public void onSocketReceived(SocketReceivedPacketEvent event) {
        String nick = event.getNick();
        if (nick.equalsIgnoreCase(mc.getSession().getUsername()))
            return;

        String text = event.getText();
        if (text.length() > 150) text = Formatting.DARK_RED + "(long message)";

        boolean isDev = false;
        for (var d : devs)
            if (d.equalsIgnoreCase(nick)) {
                isDev = true;
                break;
            }
        boolean isVip = false;
        for (var v : vips)
            if (v.equalsIgnoreCase(nick)) {
                isVip = true;
                break;
            }

        Formatting nickColor = isDev ? Formatting.LIGHT_PURPLE : (isVip ? Formatting.GREEN : Formatting.WHITE);
        String nickTag = isDev ? "[DEV] " : (isVip ? "[VIP] " : "");
        String prefixTag = (discordConfig.getValue() && receiveDiscordConfig.getValue() && nick.equalsIgnoreCase("Server"))
                ? Formatting.GRAY + "[" + Formatting.AQUA + "IRC" + Formatting.DARK_BLUE + "Discord" + Formatting.GRAY + "] "
                : Formatting.GRAY + "[" + Formatting.AQUA + "IRC" + Formatting.GRAY + "] ";

        ChatUtil.clientSendMessageRaw(prefixTag + nickColor + nickTag + nick + ": " + text + Formatting.RESET);
    }
}
