package net.shoreline.client.impl.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.shoreline.client.OvaqRebornPlus;
import net.shoreline.client.api.command.Command;
import net.shoreline.client.util.chat.ChatUtil;

import java.io.File;
import java.nio.file.Path;

/**
 * @author h_ypi
 * @since 1.0
 */

public class ConfigCommand extends Command
{

    public ConfigCommand() {
        super("Config", "Creates a new configuration preset", literal("config"));
    }

    @Override
    public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
        builder
                .then(literal("save")
                        .then(argument("config_name", StringArgumentType.string())
                                .executes(c -> {
                                    String name = StringArgumentType.getString(c, "config_name");
                                    OvaqRebornPlus.CONFIG.saveModuleConfiguration(name);
                                    ChatUtil.clientSendMessage("Saved config: §s" + name);
                                    return 1;
                                })
                        )
                )
                .then(literal("load")
                        .then(argument("config_name", StringArgumentType.string())
                                .suggests(CONFIG_SUGGESTER)
                                .executes(c -> {
                                    String name = StringArgumentType.getString(c, "config_name");
                                    if (OvaqRebornPlus.CONFIG.loadModuleConfiguration(name)) {
                                        ChatUtil.clientSendMessage("Loaded config: §s" + name);
                                    } else {
                                        ChatUtil.error("Config not found: " + name);
                                    }
                                    return 1;
                                })
                        )
                )
                .executes(c -> {
                    ChatUtil.error("Invalid usage! Usage: " + getUsage());
                    return 1;
                });
    }

    private static final SuggestionProvider<CommandSource> CONFIG_SUGGESTER = (context, builder) -> {
        Path configsDir = OvaqRebornPlus.CONFIG.getClientDirectory().resolve("Configs");
        File dir = configsDir.toFile();
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    String fname = file.getName();
                    builder.suggest(fname.substring(0, fname.length() - 5));
                }
            }
        }
        return builder.buildFuture();
    };
}
