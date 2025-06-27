package net.shoreline.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.shoreline.client.api.command.Command;
import net.shoreline.client.api.module.Module;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.chat.ChatUtil;

public class NotifyAllCommand extends Command
{
    public NotifyAllCommand()
    {
        super("NotifyAll", "Enables notify for every module", literal("notifyall"));
    }

    @Override
    public void buildCommand(LiteralArgumentBuilder<CommandSource> builder)
    {
        builder.executes(c ->
        {
            int count = 0;
            for (Module module : Managers.MODULE.getModules())
            {
                if (module instanceof ToggleModule t && !t.getNotify())
                {
                    t.setNotify(true);
                    count++;
                }
            }
            ChatUtil.clientSendMessage("Added §7" + count + "§f notifications to chat");
            return 1;
        });
    }
}
