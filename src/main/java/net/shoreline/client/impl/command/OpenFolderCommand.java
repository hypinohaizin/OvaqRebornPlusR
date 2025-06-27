package net.shoreline.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.shoreline.client.OvaqRebornPlus;
import net.shoreline.client.api.command.Command;
import net.shoreline.client.util.DesktopUtil;
import net.shoreline.client.util.chat.ChatUtil;

/**
 * @author h_ypi
 * @since 1.0
 */
public class OpenFolderCommand extends Command
{
    public OpenFolderCommand()
    {
        super("OpenFolder", "Opens the client configurations folder", literal("openfolder"));
    }

    @Override
    public void buildCommand(LiteralArgumentBuilder<CommandSource> builder)
    {
        builder.executes(c ->
        {
            try
            {
                DesktopUtil.open(OvaqRebornPlus.CONFIG.getClientDirectory().toFile());
            }
            catch (Exception e)
            {
                e.printStackTrace();
                ChatUtil.error("Failed to open client folder!");
            }
            return 1;
        });
    }
}
