package net.shoreline.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.ovaqrebornplus.client.BuildConfig;
import net.shoreline.client.api.Identifiable;
import net.shoreline.client.api.file.ClientConfiguration;
import net.shoreline.client.impl.manager.client.DiscordManager;
import net.shoreline.client.impl.manager.client.UpdateChecker;
import net.shoreline.client.impl.module.client.IRCModule;
import net.shoreline.client.init.Managers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Client main class. Handles main client mod initializing of static handler
 * instances and client managers.
 *
 * @author h_ypi
 * @see OvaqRebornPlusMod
 * @since 1.0
 */
@Environment(EnvType.CLIENT)
public class OvaqRebornPlus implements ClientModInitializer
{
    private static final Logger LOGGER = LogManager.getLogger("OvaqRebornPlus");
    // Client configuration handler. This master saves/loads the client
    // configuration files which have been saved locally.
    public static ClientConfiguration CONFIG;
    // Client shutdown hooks which will run once when the MinecraftClient
    // game instance is shutdown.
    public static ShutdownHook SHUTDOWN;
    public static DiscordManager RPC;
    public static Executor EXECUTOR;

    /**
     * Called during {@link OvaqRebornPlusMod#onInitializeClient()}
     */
    public static void init()
    {
        info("OvaqPlus Loading...");
        // Debug information - required when submitting a crash / bug repor
        info("This build of OvaqRebornPlus is on Git hash {} and was compiled on {}", BuildConfig.HASH, BuildConfig.BUILD_TIME);
        info("Starting preInit ...");

        EXECUTOR = Executors.newFixedThreadPool(1);
        info("Starting init ...");
        Managers.init();
        // Commands.init();
        info("Starting postInit ...");
        CONFIG = new ClientConfiguration();
        Managers.postInit();
        RPC = new DiscordManager();
        DiscordManager.startRPC();
        info("discordrpc starting ...");
        SHUTDOWN = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook(SHUTDOWN);
        // load configs AFTER everything has been initialized
        // this is to prevent configs loading before certain aspects of managers are available
        CONFIG.loadClient();
        if (IRCModule.getInstance().isEnabled() && !IRCModule.chat.isConnected()) {
            try {
                InetAddress address = InetAddress.getByName("www.google.com");
                boolean isReachable = address.isReachable(5000);
                if (isReachable) {
                    info("Connecting to IRC Server (Init)");
                    IRCModule.chat.connect();
                } else {
                    info("Network Connection Error. (Maybe Bug");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        info("OvaqPlus Loadead!");
    }

    @Override
    public void onInitializeClient() {
        init();
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void info(String message, Object ... params) {
        LOGGER.info(message, params);
    }

    public static void info(Identifiable feature, String message) {
        LOGGER.info(String.format("[%s] %s", feature.getId(), message));
    }

    public static void info(Identifiable feature, String message, Object ... params) {
        LOGGER.info(String.format("[%s] %s", feature.getId(), message), params);
    }

    public static void error(String message) {
        LOGGER.error(message);
    }

    public static void error(String message, Object ... params) {
        LOGGER.error(message, params);
    }

    public static void error(Identifiable feature, String message) {
        LOGGER.error(String.format("[%s] %s", feature.getId(), message));
    }

    public static void error(Identifiable feature, String message, Object ... params) {
        LOGGER.error(String.format("[%s] %s", feature.getId(), message), params);
    }
}