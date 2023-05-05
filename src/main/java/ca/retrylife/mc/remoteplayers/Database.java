package ca.retrylife.mc.remoteplayers;

import ca.retrylife.mc.remoteplayers.config.RemotePlayerConfig;
import me.shedaniel.autoconfig.AutoConfig;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent storage for the application
 */
public class Database {
    private static Database instance = null;

    private final RemotePlayerConfig config;

    public static Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    private Database() {
        config = AutoConfig.getConfigHolder(RemotePlayerConfig.class).getConfig();
    }

    /**
     * Gets if waypoint integration should be enabled
     */
    public boolean modEnabled() {
        return config.modEnabled;
    }

    public boolean entityRadarEnabled() {
        return config.entityRadarEnabled;
    }

    public boolean inGameWaypointsEnabled() {
        return config.inGameWaypointsEnabled;
    }

    public int waypointColor() {
        return config.waypointColor.ordinal();
    }

    public boolean showOverworldPositionInNether() {
        return config.showOverworldPositionInNether;
    }

    public boolean showNetherPositionInOverworld() {
        return config.showNetherPositionInOverworld;
    }

    public int updateInterval() {
        return config.updateInterval;
    }

    public int minimumWaypointDistance() {
        return config.minimumWaypointDistance;
    }

    public int minimumVisibleWaypointDistance() {
        return config.minimumVisibleWaypointDistance;
    }

    public RemotePlayerConfig.ChatNotificationType getChatNotificationType() {
        return config.chatNotificationType;
    }

    /**
     * Get the dynmap url for a minecraft server. May be null
     * 
     * @param server Minecraft server IP
     * @return Dynmap URL
     */
    public @Nullable String getConfiguredDynmapForServer(String server) {
        return config.serverDynmapURLs.get(server);
    }

    /**
     * Check if there is a dynmap server linked to a minecraft server
     * 
     * @param server Minecraft server
     * @return Does link exist?
     */
    public boolean serverHasDynmapLinked(String server) {
        return getConfiguredDynmapForServer(server) != null;
    }

}