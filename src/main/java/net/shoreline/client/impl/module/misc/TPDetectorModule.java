package net.shoreline.client.impl.module.misc;

import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.util.chat.ChatUtil;
import net.shoreline.eventbus.annotation.EventListener;

/**
 * @author h_ypi
 * @since 1.0
 */
public class TPDetectorModule extends ToggleModule
{
    private static TPDetectorModule INSTANCE;

    public TPDetectorModule()
    {
        super("TPDetector", "Notifies you whenever a teleport occurs", ModuleCategory.MISCELLANEOUS);
        INSTANCE = this;
    }

    public static TPDetectorModule getInstance()
    {
        return INSTANCE;
    }

    @EventListener
    public void onPlayerListPacket(PlayerListS2CPacket p)
    {
        int i = 0;
        for (PlayerListS2CPacket.Action action : p.getActions())
        {
            if (action == PlayerListS2CPacket.Action.ADD_PLAYER)
            {
                PlayerListS2CPacket.Entry entry = p.getEntries().get(i);
                ChatUtil.clientSendMessage("§d[TPDetector] §fPossible teleport detected: §a%s §7(gamemode: %s, listed: %s)"
                        .formatted(entry.profile().getName(), entry.gameMode(), entry.listed()));
            }
            i++;
        }
    }
}