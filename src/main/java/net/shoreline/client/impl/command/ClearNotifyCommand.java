package net.shoreline.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.shoreline.client.api.command.Command;
import net.shoreline.client.api.module.Module;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.chat.ChatUtil;

public class ClearNotifyCommand extends Command
{
    public ClearNotifyCommand()
    {
        super("ClearNotify", "Disables notify for every module", literal("clearnotify"));
    }

    @Override
    public void buildCommand(LiteralArgumentBuilder<CommandSource> builder)
    {
        builder.executes(c ->
        {
            int count = 0;
            for (Module module : Managers.MODULE.getModules())
            {
                if (module instanceof ToggleModule t && t.getNotify())
                {
                    t.setNotify(false);
                    count++;
                }
            }
            ChatUtil.clientSendMessage("Removed §7" + count + "§f notifications from chat");
            return 1;
        });
    }
}
