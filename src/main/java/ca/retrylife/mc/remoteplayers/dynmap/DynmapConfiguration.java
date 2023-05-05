package ca.retrylife.mc.remoteplayers.dynmap;

/**
 * JSON object from dynmap API. Used to read the default world name.
 */
public class DynmapConfiguration {
    public String defaultworld;

    public static class World {
        public String name;
    }

    public World[] worlds;
}