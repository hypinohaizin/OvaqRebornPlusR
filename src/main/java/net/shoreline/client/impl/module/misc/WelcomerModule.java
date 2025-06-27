package net.shoreline.client.impl.module.misc;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.gui.chat.ChatMessageEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.util.chat.ChatUtil;
import net.shoreline.eventbus.annotation.EventListener;

import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WelcomerModule extends ToggleModule {
    Config<Boolean> joinsConfig = register(new BooleanConfig("Joins", "Sends messages when a player joins the server", true));
    Config<Boolean> leavesConfig = register(new BooleanConfig("Leaves", "ends messages when a player leaves the server", true));
    Config<Boolean> clientsideConfig = register(new BooleanConfig("Clientside", "Sends the messages only on your side.", false));
    Config<Boolean> unicodeConfig = register(new BooleanConfig("Unicode", "Uses prettier unicode icons instead of normal arrows", false));
    Config<Boolean> greenTextConfig = register(new BooleanConfig("GreenText", "Makes your message green.", false));
    Config<Integer> delayConfig = register(new NumberConfig<>("Delay", "The delay for the announcer.", 5, 0, 30));

    private final String[] JOIN_MESSAGES = {
            "Hello, ", "Welcome to the server, ", "Good to see you, ",
            "Greetings, ", "Good evening, ", "Hey, "
    };
    private final String[] LEAVE_MESSAGES = {
            "Goodbye, ", "See you later, ", "Bye bye, ",
            "I hope you had a good time, ", "Farewell, ", "See you next time, "
    };

    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
    private long lastSendTime = 0L;
    private final Random random = new Random();

    private final Set<java.util.UUID> knownPlayers = new HashSet<>();

    public WelcomerModule() {
        super("Welcomer", "Sends a message when a player joins or leaves the server.", ModuleCategory.MISCELLANEOUS);
    }

    @Override
    public void onEnable() {
        queue.clear();
        lastSendTime = System.currentTimeMillis();
        knownPlayers.clear();
        if (mc.getNetworkHandler() != null) {
            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                knownPlayers.add(entry.getProfile().getId());
            }
        }
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!queue.isEmpty() && !clientsideConfig.getValue()) {
            long now = System.currentTimeMillis();
            if (now - lastSendTime > delayConfig.getValue() * 1000L) {
                String message = queue.poll();
                if (greenTextConfig.getValue()) message = "> " + message;
                mc.player.networkHandler.sendChatMessage(message);
                lastSendTime = now;
            }
        }
    }

    @EventListener
    public void onGameChat(ChatMessageEvent event) {
        lastSendTime = System.currentTimeMillis();
    }

    @EventListener
    public void onPacketInbound(PacketEvent.Inbound event) {
        if (mc.player == null || mc.world == null) return;

        if (event.getPacket() instanceof PlayerListS2CPacket packet && joinsConfig.getValue()) {
            if (packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) {
                for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
                    GameProfile profile = entry.profile();
                    if (profile == null || profile.getName() == null) continue;
                    if (profile.getId().equals(mc.player.getUuid())) continue;
                    if (knownPlayers.add(profile.getId())) {
                        String name = profile.getName();
                        if (clientsideConfig.getValue()) {
                            String prefix = Formatting.DARK_GRAY + "[" + Formatting.GREEN + (unicodeConfig.getValue() ? "»" : ">") + Formatting.DARK_GRAY + "] " + Formatting.GRAY;
                            String joinMsg = JOIN_MESSAGES[random.nextInt(JOIN_MESSAGES.length)] + name;
                            ChatUtil.clientSendMessage(prefix + joinMsg);
                        } else {
                            String joinMsg = JOIN_MESSAGES[random.nextInt(JOIN_MESSAGES.length)] + name;
                            queue.add(joinMsg);
                        }
                    }
                }
            }
        }

        if (event.getPacket() instanceof PlayerRemoveS2CPacket packet && leavesConfig.getValue()) {
            for (java.util.UUID uuid : packet.profileIds()) {
                if (uuid.equals(mc.player.getUuid())) continue;
                if (knownPlayers.remove(uuid)) {
                    String name = null;
                    if (mc.getNetworkHandler() != null) {
                        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                            if (entry.getProfile().getId().equals(uuid)) {
                                name = entry.getProfile().getName();
                                break;
                            }
                        }
                    }
                    if (name == null) name = uuid.toString();
                    if (clientsideConfig.getValue()) {
                        String prefix = Formatting.DARK_GRAY + "[" + Formatting.RED + (unicodeConfig.getValue() ? "«" : "<") + Formatting.DARK_GRAY + "] " + Formatting.GRAY;
                        String leaveMsg = LEAVE_MESSAGES[random.nextInt(LEAVE_MESSAGES.length)] + name;
                        ChatUtil.clientSendMessage(prefix + leaveMsg);
                    } else {
                        String leaveMsg = LEAVE_MESSAGES[random.nextInt(LEAVE_MESSAGES.length)] + name;
                        queue.add(leaveMsg);
                    }
                }
            }
        }
    }
}
