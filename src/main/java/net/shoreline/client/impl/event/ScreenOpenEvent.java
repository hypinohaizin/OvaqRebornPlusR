package net.shoreline.client.impl.event;

import net.minecraft.client.gui.screen.Screen;
import net.shoreline.eventbus.event.Event;

/**
 * @author h_ypi
 * @since 1.0
 */
public class ScreenOpenEvent extends Event
{
    //
    private final Screen screen;

    public ScreenOpenEvent(Screen screen)
    {
        this.screen = screen;
    }

    public Screen getScreen()
    {
        return screen;
    }
}
