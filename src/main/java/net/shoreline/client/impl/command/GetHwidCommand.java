package net.shoreline.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.shoreline.client.api.command.Command;
import net.shoreline.client.impl.manager.client.HwidManager;
import net.shoreline.client.util.chat.ChatUtil;

import java.awt.*;

/**
 * @author h_ypi
 * @since 1.0
 */
public class GetHwidCommand extends Command {

    /**
     *
     */
    public GetHwidCommand() {
        super("GetHwid", "get hwid", literal("gethwid"));
    }

    @Override
    public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(c -> {
            try {
                String hwid = HwidManager.getHWID();
                Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(new java.awt.datatransfer.StringSelection(hwid), null);

                ChatUtil.clientSendMessage("Your HWID has been copied to clipboard: " + hwid);
            } catch (Exception e) {
                e.printStackTrace();
                ChatUtil.clientSendMessage("Failed to get or copy HWID");
            }
            return 1;
        });
    }
}
