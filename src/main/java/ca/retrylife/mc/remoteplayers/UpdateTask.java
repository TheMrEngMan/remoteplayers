package ca.retrylife.mc.remoteplayers;

import ca.retrylife.mc.remoteplayers.config.RemotePlayerConfig;
import ca.retrylife.mc.remoteplayers.dynmap.DynmapConnection;
import ca.retrylife.mc.remoteplayers.dynmap.PlayerPosition;
import ca.retrylife.mc.remoteplayers.utils.ChatUtil;
import ca.retrylife.mc.remoteplayers.utils.PlayerWaypoint;
import com.google.gson.JsonSyntaxException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Language;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointSet;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaero.common.mods.SupportXaeroWorldmap;
import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;

import java.io.IOException;
import java.util.*;

/**
 * Threaded task that is run once every second to update the local maps and fetch data from dynmap when need be
 */
public class UpdateTask extends TimerTask {
    private final Logger logger = LogManager.getLogger(getClass());

    // Access to minecraft and map
    private final MinecraftClient mc;
    private SupportXaeroWorldmap supportXaeroWorldmap;

    // For keeping track of how often to request new data from Dynmap
    // When this reaches 0, new data is requested
    private static int dynmapUpdateCounter = 0;

    // For keeping track of players in the overworld / nether
    public static String currentWorldMapName = "";

    // For keeping track of the actual player's positions obtained from Dynmap
    public static HashMap<String, PlayerPosition> playerPositions;
    public static HashMap<String, PlayerPosition> previousPlayerPositions;

    // For keeping track of when different worlds are loaded by the map in order to properly update the in-game waypoints
    public static String currentWaypointWorldID = "";
    public static String previousWaypointWorldID = "";

    // For keeping track of when players appear / disappear on/from the map
    public static ArrayList<String> onlinePlayers = new ArrayList<>();
    public static ArrayList<String> previousOnlinePlayers = new ArrayList<>();

    // For keeping track of names of waypoints that have been added by the mod in order to remove them and not any other user-created waypoints
    private static final HashSet<String> allCreatedPlayerWaypointNames = new HashSet<>();

    public UpdateTask() {
        this.mc = MinecraftClient.getInstance();
    }

    private WaypointWorld getCurrentWaypointWorld() throws NullPointerException {
        return XaeroMinimapSession.getCurrentSession().getWaypointsManager().getCurrentWorld();
    }

    private ArrayList<Waypoint> getWaypointList() throws NullPointerException {
        return getCurrentWaypointWorld().getCurrentSet().getList();
    }
    private HashMap<String, WaypointSet> getAllWaypointSets() throws NullPointerException {
        return getCurrentWaypointWorld().getSets();
    }

    public void onConfigChange() {
        if(!Database.getInstance().modEnabled()) {
            // Remove all the player positions if mod got disabled
            if(playerPositions != null) playerPositions.clear();
        } else {
            // Otherwise, check if need to enable
            RemotePlayers.enabled = Database.getInstance().serverHasDynmapLinked(RemotePlayers.currentServerIP);
        }
        recreateWaypoints();
        updateNow();
    }

    public void recreateWaypoints() {
        HashMap<String, WaypointSet> waypointSets;
        try {
            waypointSets = getAllWaypointSets();
        } catch (NullPointerException e) {
            // Skip if minimap not available yet
            return;
        }
        for(WaypointSet waypointSet : waypointSets.values()) {
            ArrayList<Waypoint> waypointList = waypointSet.getList();
            synchronized (waypointList) {
                // For each of the configured waypoint sets, loop though all waypoints and remove the waypoints created by the mod
                waypointList.removeIf(waypoint -> waypoint.isTemporary() && allCreatedPlayerWaypointNames.contains(waypointNameToPlayerName(waypoint.getName())));
            }
        }
    }

    public void updateNow() {
        // If an update is requested immediately, get Dynmap data immediately
        dynmapUpdateCounter = 0;
        run();
    }

    public void onDisconnect() {
        // Remove all the player positions when disconnecting from server
        if(playerPositions != null) playerPositions.clear();
    }

    public void run() {
        try {
            // Catch any possible exceptions to make sure update timer doesn't stop
            update();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void update() {

        // Skip if world, player, or camera aren't available
        if(mc.world == null || mc.player == null || mc.cameraEntity == null) return;

        // Skip if not enabled
        if(!Database.getInstance().modEnabled() || !RemotePlayers.enabled) {
            return;
        }

        // Handle setting up a new connection to Dynmap
        if (RemotePlayers.getConnection() == null) {
            try {
                RemotePlayers.setConnection(new DynmapConnection(Objects.requireNonNull(Database.getInstance().getConfiguredDynmapForServer(RemotePlayers.currentServerIP))));
            } catch (Exception e) {
                e.printStackTrace();
                String helpText = "(Unknown error)";
                if(e instanceof JsonSyntaxException) {
                    helpText = "(You may need https instead of http or vise-versa)";
                }
                if(e instanceof IOException) {
                    helpText = "(Check that you have entered the correct URL)";
                }
                ChatUtil.showErrorChatMessage(mc.player, e, "Error connecting to Dynmap!", helpText);
                RemotePlayers.setConnection(null);
                RemotePlayers.enabled = false;
                return;
            }
        }

        // Skip if minimap not available yet
        // This can happen when the map is not fully loaded yet, so don't disconnect from Dynmap
        WaypointWorld currentWaypointWorld;
        try {
            currentWaypointWorld = getCurrentWaypointWorld();
            previousWaypointWorldID = currentWaypointWorldID;
            currentWaypointWorldID = currentWaypointWorld.getFullId();
        } catch (Exception e) {
            logger.warn("Map not loaded yet");
            return;
        }

        // Get a list of all player's positions if need be
        if(dynmapUpdateCounter == 0) {
            try {
                if (playerPositions != null) {
                    previousPlayerPositions = (HashMap<String, PlayerPosition>) playerPositions.clone();
                }
                playerPositions = RemotePlayers.getConnection().getAllPlayerPositions();
            } catch (IOException e) {
                logger.warn("Could not get data from Dynmap");
                e.printStackTrace();
                ChatUtil.showErrorChatMessage(mc.player, e, "Error getting data from Dynmap!", "(Could not get data from Dynmap)");
                RemotePlayers.setConnection(null);
                RemotePlayers.enabled = false;
                return;
            }

            // Update the player positions obtained from Dynmap with GameProfile data from the actual logged-in players
            // This is required so that the entity radar properly shows the player's skin on player head icons
            if(Database.getInstance().entityRadarEnabled()) {
                Collection<PlayerListEntry> playerList = mc.getNetworkHandler().getPlayerList();
                previousOnlinePlayers = (ArrayList<String>) onlinePlayers.clone();
                onlinePlayers.clear();
                for (PlayerListEntry playerListEntity : playerList) {
                    String playerName = playerListEntity.getProfile().getName();
                    onlinePlayers.add(playerName);
                    if (playerPositions.containsKey(playerName)) {
                        if (playerListEntity.getProfile() != null) {
                            playerPositions.get(playerName).setPlayerGameProfile(playerListEntity.getProfile());
                        }
                    }
                }
            }

            // Check if any players appeared on the map (while already being logged in) or
            // disappeared from the map (while still being logged in) or changed worlds
            // TODO: fix this so it doesn't *sometimes* show appear / disappear messages as *some* players log in/out and when joining a server (for *some* players)
            if(previousPlayerPositions != null && Database.getInstance().getChatNotificationType() != RemotePlayerConfig.ChatNotificationType.NONE) {
                for (PlayerPosition previousPlayerPosition : previousPlayerPositions.values()) {
                    // Don't show notifications for self
                    if(previousPlayerPosition.username.equals(mc.player.getEntityName())) continue;
                    // Don't show notifications for invisible players
                    if(previousPlayerPosition.worldName.equals(DynmapConnection.BOGUS_WORLD_NAME)) continue;
                    if (!playerPositions.containsKey(previousPlayerPosition.username) && onlinePlayers.contains(previousPlayerPosition.username)) {
                        ChatUtil.showNotificationChatMessage(mc.player, Text.of(String.format(Language.getInstance().get("text.remoteplayers.chat.nofification.playerdisappeared").replaceAll("§<cc>", "§" + Integer.toHexString(Database.getInstance().waypointColor())), previousPlayerPosition.username, previousPlayerPosition.worldName)));
                    } else {
                        if (playerPositions.containsKey(previousPlayerPosition.username)  && !playerPositions.get(previousPlayerPosition.username).worldName.equals(previousPlayerPosition.worldName)) {
                            ChatUtil.showNotificationChatMessage(mc.player, Text.of(String.format(Language.getInstance().get("text.remoteplayers.chat.nofification.playermoved").replaceAll("§<cc>", "§" + Integer.toHexString(Database.getInstance().waypointColor())), previousPlayerPosition.username, previousPlayerPosition.worldName, playerPositions.get(previousPlayerPosition.username).worldName)));
                        }
                    }
                }
                for (PlayerPosition playerPosition : playerPositions.values()) {
                    // Don't show notifications for self
                    if(playerPosition.username.equals(mc.player.getEntityName())) continue;
                    // Don't show notifications for invisible players
                    if(playerPosition.worldName.equals(DynmapConnection.BOGUS_WORLD_NAME)) continue;
                    if (!previousPlayerPositions.containsKey(playerPosition.username) && previousOnlinePlayers.contains(playerPosition.username) && onlinePlayers.contains(playerPosition.username)) {
                        ChatUtil.showNotificationChatMessage(mc.player, Text.of(String.format(Language.getInstance().get("text.remoteplayers.chat.nofification.playerappeared").replaceAll("§<cc>", "§" + Integer.toHexString(Database.getInstance().waypointColor())), playerPosition.username, playerPosition.worldName)));
                    }
                }
            }

            // Reset the counter back to the interval to request new data at
            dynmapUpdateCounter = Database.getInstance().updateInterval();
        }
        // Otherwise, if it's not time to request new data yet, count down to the next time new data needs to be requested
        else {
            dynmapUpdateCounter--;
        }

        // Update in-game waypoints if configured to do so
        ArrayList<Waypoint> waypointList;
        if (Database.getInstance().inGameWaypointsEnabled()) {

            // Fetch the correct list of waypoints
            waypointList = getWaypointList();

            if(waypointList == null) {
                logger.warn("Could not get waypoints list");
                return;
            }

            // Get the name of the current world the player is in via the configured world name in Xaero's world map
            // This is required for displaying players in the current world (and corresponding nether / overworld)
            WorldMapSession worldmapSession = WorldMapSession.getCurrentSession();
            if (worldmapSession != null && worldmapSession.isUsable()) {
                MapProcessor mapProcessor = worldmapSession.getMapProcessor();
                try {
                    if(supportXaeroWorldmap == null) {
                        supportXaeroWorldmap = new SupportXaeroWorldmap(XaeroMinimapSession.getCurrentSession().getModMain());
                    }
                    currentWorldMapName = supportXaeroWorldmap.tryToGetMultiworldName(mc.player.getWorld().getRegistryKey(), mapProcessor.getCurrentMWId());
                    if(currentWorldMapName == null) throw new NullPointerException();
                } catch (Exception e) {
                    logger.warn("Could not get current world name!");
                    return;
                }
            }

            // Determine the corresponding nether / overworld name, if applicable
            String currentWorldCorrespondingOverworldName = currentWorldMapName.contains("_nether") ? currentWorldMapName.replace("_nether", "") : "";
            String currentWorldCorrespondingNetherName = !currentWorldMapName.contains("_nether") ? currentWorldMapName + "_nether" : "";

            // If player switched worlds loaded by the map, need to re-create the waypoints to properly update them
            if(!previousWaypointWorldID.isEmpty() && !previousWaypointWorldID.equals(currentWaypointWorldID)) {
                recreateWaypoints();
            }

            try {
                synchronized (waypointList) {

                    // Create indexes of matching player names to waypoints to update the waypoints by index
                    HashMap<String, Integer> waypointNamesIndexes = new HashMap<>(waypointList.size());
                    for (int i = 0; i < waypointList.size(); i++) {
                        Waypoint waypoint = waypointList.get(i);
                        waypointNamesIndexes.put(waypointNameToPlayerName(waypoint.getName()), i);
                    }

                    // Create indexes of matching player names to player client entities to get distances to each player in range by index
                    List<AbstractClientPlayerEntity> playerClientEntityList = mc.world.getPlayers();
                    HashMap<String, Integer> playerClientEntityIndexes = new HashMap<>(waypointList.size());
                    for (int i = 0; i < playerClientEntityList.size(); i++) {
                        AbstractClientPlayerEntity playerClientEntity = playerClientEntityList.get(i);
                        playerClientEntityIndexes.put(playerClientEntity.getEntityName(), i);
                    }

                    // Keep track of which waypoints were previously shown to remove any that are not to be shown anymore
                    ArrayList<String> currentPlayerWaypointNames = new ArrayList<>();

                    // Add each player to the map
                    for (PlayerPosition playerPosition : playerPositions.values()) {
                        boolean showCurrentPlayersOverworldPositionInNether = false;
                        boolean showCurrentPlayersNetherPositionInOverworld = false;
                        String playerName = playerPosition.username;

                        // If this player is self, don't show waypoint
                        if (mc.player.getName() != null && playerName.equals(mc.player.getEntityName())) continue;

                        // If the player is in a different world, and not configured to show overworld positions in nether or vise-versa, don't show waypoint
                        if(!Database.getInstance().showOverworldPositionInNether() && !Database.getInstance().showNetherPositionInOverworld()) {
                            if (!playerPosition.worldName.equals(currentWorldMapName)) continue;
                        }
                        // Otherwise, if configured to show overworld positions in nether or vise-versa, only show if the player is in the corresponding world or same world
                        else {
                            if (Database.getInstance().showOverworldPositionInNether() && playerPosition.worldName.equals(currentWorldCorrespondingOverworldName)) {
                                showCurrentPlayersOverworldPositionInNether = true;
                            }
                            else if (Database.getInstance().showNetherPositionInOverworld() && playerPosition.worldName.equals(currentWorldCorrespondingNetherName)) {
                                showCurrentPlayersNetherPositionInOverworld = true;
                            }
                            else if(!playerPosition.worldName.equals(currentWorldMapName)) {
                                continue;
                            }
                        }

                        // Check if this player is within the server's player entity tracking range
                        if (playerClientEntityIndexes.containsKey(playerName)) {

                            AbstractClientPlayerEntity playerClientEntity = playerClientEntityList.get(playerClientEntityIndexes.get(playerName));
                            int minimumWaypointDistanceToUse = Database.getInstance().minimumWaypointDistance();
                            int minimumVisibleWaypointDistanceToUse = Database.getInstance().minimumVisibleWaypointDistance();
                            if(minimumWaypointDistanceToUse > minimumVisibleWaypointDistanceToUse) minimumVisibleWaypointDistanceToUse = minimumWaypointDistanceToUse;

                            // If in range of the minimum visible waypoint distance, potentially need to check if visible
                            if(minimumVisibleWaypointDistanceToUse > 0 && playerClientEntity.isInRange(mc.cameraEntity, minimumVisibleWaypointDistanceToUse)) {

                                if(minimumWaypointDistanceToUse > 0) {
                                    // If closer than the minimum waypoint distance, don't show waypoint
                                    if (playerClientEntity.isInRange(mc.cameraEntity, minimumWaypointDistanceToUse)) {
                                        continue;
                                    }
                                    // Otherwise, need to check if player is visible
                                    else {
                                        RaycastContext raycastContext = new RaycastContext(mc.cameraEntity.getPos(), playerClientEntity.getPos(), RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.ANY, mc.cameraEntity);
                                        // If this player is visible, don't show waypoint
                                        if (mc.world.raycast(raycastContext).getType() != HitResult.Type.BLOCK) {
                                            continue;
                                        }
                                    }
                                }

                            }

                        }

                        // If a waypoint for this player already exists, update it
                        if (waypointNamesIndexes.containsKey(playerName)) {
                            Waypoint waypoint = waypointList.get(waypointNamesIndexes.get(playerName));

                            // Scale the coordinates if showing player in corresponding nether / overworld
                            double coordinateMultiplier = 1;
                            if(showCurrentPlayersOverworldPositionInNether) {
                                coordinateMultiplier = 1d / RemotePlayers.NETHER_COORDINATE_SCALE;
                            }
                            else if (showCurrentPlayersNetherPositionInOverworld) {
                                coordinateMultiplier = RemotePlayers.NETHER_COORDINATE_SCALE;
                            }

                            waypoint.setX((int)(playerPosition.x * coordinateMultiplier));
                            waypoint.setY(playerPosition.y - 1);
                            waypoint.setZ((int)(playerPosition.z * coordinateMultiplier));

                            // Append (N) or (OW) to name and asterisk to symbol if showing player in corresponding nether / overworld
                            waypoint.setName(playerName + (showCurrentPlayersOverworldPositionInNether ? " (OW)" : "") + (showCurrentPlayersNetherPositionInOverworld ? " (N)" : ""));
                            waypoint.setSymbol(waypoint.getSymbol().charAt(0) + (showCurrentPlayersOverworldPositionInNether || showCurrentPlayersNetherPositionInOverworld ? "*" : ""));
                            currentPlayerWaypointNames.add(waypointNameToPlayerName(waypoint.getName()));
                        }

                        // Otherwise, add a waypoint for the player
                        else {
                            try {
                                PlayerWaypoint currentPlayerWaypoint = new PlayerWaypoint(playerPosition, showCurrentPlayersOverworldPositionInNether ? " (OW)" : (showCurrentPlayersNetherPositionInOverworld ? " (N)" : ""));
                                waypointList.add(currentPlayerWaypoint);
                                currentPlayerWaypointNames.add(waypointNameToPlayerName(currentPlayerWaypoint.getName()));
                                allCreatedPlayerWaypointNames.add(waypointNameToPlayerName(currentPlayerWaypoint.getName()));
                            } catch (NullPointerException ignored) {}
                        }

                    }

                    // Remove any waypoints for players not shown on map anymore
                    waypointList.removeIf(waypoint -> waypoint.isTemporary() && allCreatedPlayerWaypointNames.contains(waypoint.getName()) && !currentPlayerWaypointNames.contains(waypoint.getName()));
                    // Remove any waypoints for players in other dimensions if not configured to anymore
                    if(!Database.getInstance().showOverworldPositionInNether()) waypointList.removeIf(waypoint -> waypoint.isTemporary() && waypoint.getName().contains(" (OW)") && playerPositions.values().stream().anyMatch(p -> p.username.equals(waypointNameToPlayerName(waypoint.getName()))));
                    if(!Database.getInstance().showNetherPositionInOverworld()) waypointList.removeIf(waypoint -> waypoint.isTemporary() && waypoint.getName().contains(" (N)") && playerPositions.values().stream().anyMatch(p -> p.username.equals(waypointNameToPlayerName(waypoint.getName()))));

                }
            } catch (ConcurrentModificationException ignored) {}

        }

    }

    private String waypointNameToPlayerName(String waypointName) {
        return waypointName.replace(" (OW)", "").replace(" (N)", "");
    }

}