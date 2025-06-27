package net.shoreline.client;

import net.shoreline.client.api.file.ClientConfiguration;
import net.shoreline.client.impl.module.client.IRCModule;

/**
 * @author h_ypi
 * @since 1.0
 */
public class ShutdownHook extends Thread
{
    /**
     *
     */
    public ShutdownHook()
    {
        setName("ovaqrebornplus-ShutdownHook");
    }

    /**
     * This runs when the game is shutdown and saves the
     * {@link ClientConfiguration} files.
     *
     * @see ClientConfiguration#saveClient()
     */
    @Override
    public void run()
    {
        OvaqRebornPlus.info("Saving configurations and shutting down!");
        OvaqRebornPlus.CONFIG.saveClient();
        OvaqRebornPlus.CONFIG.saveClickGui();
        OvaqRebornPlus.CONFIG.saveFonts();
        OvaqRebornPlus.RPC.stopRPC();
        if (IRCModule.getInstance().isEnabled() && IRCModule.chat.isConnected()) {
            IRCModule.chat.disconnect();
        }
    }
}
