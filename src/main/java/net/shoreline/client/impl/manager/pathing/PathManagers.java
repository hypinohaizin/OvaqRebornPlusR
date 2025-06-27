package net.shoreline.client.impl.manager.pathing;

/**
 * @author h_ypi
 * @since 1.0
 */

public class PathManagers {
    private static IPathManager manager;
    public static IPathManager get() {
        if (manager == null) {
            try {
                Class.forName("baritone.api.BaritoneAPI");
                manager = new BaritonePathManager();
            } catch (Throwable t) {
                manager = new NopPathManager();
            }
        }
        return manager;
    }
}
