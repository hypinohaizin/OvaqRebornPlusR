package net.shoreline.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.shoreline.client.OvaqRebornPlus;
import net.shoreline.client.api.command.Command;
import net.shoreline.client.util.chat.ChatUtil;

public class LoadCommand extends Command
{
    public LoadCommand()
    {
        super("Load", "Loads the current config", literal("load"));
    }

    @Override
    public void buildCommand(LiteralArgumentBuilder<CommandSource> builder)
    {
        builder.executes(c ->
        {
            OvaqRebornPlus.CONFIG.loadClientModules();
            ChatUtil.clientSendMessage("Loaded client config!");
            return 1;
        });
    }
}
