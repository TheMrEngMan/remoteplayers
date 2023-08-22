package ca.retrylife.mc.remoteplayers;

import ca.retrylife.mc.remoteplayers.commands.PlayerCommands;
import ca.retrylife.mc.remoteplayers.config.ServerDynmapURLGuiProvider;
import ca.retrylife.mc.remoteplayers.config.RemotePlayerConfig;
import ca.retrylife.mc.remoteplayers.config.WaypointColorGuiProvider;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.event.ConfigSerializeEvent;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;

import java.util.Timer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ca.retrylife.mc.remoteplayers.dynmap.DynmapConnection;

/**
 * Mod entrypoint. Sets everything up
 */
public class RemotePlayers implements ModInitializer {
    private static Logger logger = LogManager.getLogger(RemotePlayers.class);

    // Update task
    private static final UpdateTask updateTask = new UpdateTask();

    // Connection to Dynmap
    private static DynmapConnection connection = null;

    // Status
    public static boolean enabled = false;
    public static String currentServerIP;

    // TODO: Maybe save last known position (optionally as permanent waypoints if player leaves)
    // TODO: Don't flood all waypoint lists with temp waypoints when selecting current active waypoint set in world map

    // TODO: Somehow make this adapt to the actual coordinate scale (using coordinateScale())
    public static final int NETHER_COORDINATE_SCALE = 8;

    @Override
    public void onInitialize() {
        // Load and setup the configuration (ModMenu integration)
        loadConfig();
        // Schedule updating the map
        Timer updateThread = new Timer(true);
        updateThread.scheduleAtFixedRate(updateTask, 0, 1000);
        // Register connection / disconnection events
        ConnectionHandler.init();
        // Register the commands
        PlayerCommands.register();
    }

    public void loadConfig() {
        AutoConfig.register(RemotePlayerConfig.class, GsonConfigSerializer::new);
        var guiRegistry = AutoConfig.getGuiRegistry(RemotePlayerConfig.class);
        // Create and register custom providers for the server IP -> Dynmap URL list
        guiRegistry.registerPredicateProvider(
                new ServerDynmapURLGuiProvider(),
                field -> field.getName().equals("serverDynmapURLs")
        );
        // And the waypoint color selection
        guiRegistry.registerPredicateProvider(
                new WaypointColorGuiProvider(),
                field -> field.getName().equals("waypointColor")
        );
        AutoConfig.getConfigHolder(RemotePlayerConfig.class).registerSaveListener(onConfigSaved());
    }

    public ConfigSerializeEvent.Save<RemotePlayerConfig> onConfigSaved() {
        return (configHolder, remotePlayerConfig) -> {
            // When user changes any config value, reset everything to apply the new values
            setConnection(null);
            updateTask.onConfigChange();
            return null;
        };
    }

    public static void onDisconnect() {
        // Remove Dynmap connection data when disconnecting from server
        RemotePlayers.setConnection(null);
        updateTask.onDisconnect();
    }

    /**
     * Sets the current dynmap connection
     * 
     * @param connection Connection
     */
    public static void setConnection(DynmapConnection connection) {
        RemotePlayers.connection = connection;
    }

    /**
     * Gets the current dynmap connection
     * 
     * @return Connection
     */
    public static @Nullable DynmapConnection getConnection() {
        return RemotePlayers.connection;
    }

}