package net.shoreline.client.init;

import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.shoreline.client.OvaqRebornPlus;
import net.shoreline.client.impl.font.AWTFontRenderer;
import net.shoreline.client.impl.font.VanillaTextRenderer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Fonts
{
    public static final VanillaTextRenderer VANILLA = new VanillaTextRenderer();

    public static final String DEFAULT_FONT_FILE_PATH = "assets/ovaqrebornplus/font/hackgen.ttf";
    public static String FONT_FILE_PATH = "assets/ovaqrebornplus/font/hackgen.ttf";

    public static AWTFontRenderer CLIENT;
    public static AWTFontRenderer CLIENT_UNSCALED;
    public static float FONT_SIZE = 9.0f;

    private static boolean initialized;

    public static void init()
    {
        if (initialized)
        {
            return;
        }
        OvaqRebornPlus.CONFIG.loadFonts();
        Fonts.loadFonts();
        OvaqRebornPlus.info("Loaded fonts!");
        initialized = true;
    }

    public static void loadFonts() {
        try {
            Identifier identifier = Identifier.of("ovaqrebornplus", "font/hackgen.ttf");
            InputStream stream1 = MinecraftClient.getInstance().getResourceManager().getResource(identifier).get().getInputStream();
            CLIENT = new AWTFontRenderer(stream1, FONT_SIZE);

            InputStream stream2 = MinecraftClient.getInstance().getResourceManager().getResource(identifier).get().getInputStream();
            CLIENT_UNSCALED = new AWTFontRenderer(stream2, 9.0f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void closeFonts()
    {
        CLIENT.close();
        CLIENT_UNSCALED.close();
    }

    public static void setSize(float size)
    {
        FONT_SIZE = size;
        try
        {
            CLIENT = new AWTFontRenderer(new FileInputStream(FONT_FILE_PATH), FONT_SIZE);
        }
        catch (IOException e)
        {
            // mhm
        }
    }

    public static boolean isInitialized()
    {
        return initialized;
    }
}