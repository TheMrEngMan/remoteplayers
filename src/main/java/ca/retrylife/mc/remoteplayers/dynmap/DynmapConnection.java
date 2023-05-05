package ca.retrylife.mc.remoteplayers.dynmap;

import ca.retrylife.mc.remoteplayers.utils.HTTP;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * Represents a connection to a dynmap server
 */
public class DynmapConnection {

    // URL
    private URL queryURL;

    private final DynmapConfiguration dynmapConfiguration;
    private final String baseURLString;

    /**
     * Create a new dynmap connection
     * 
     * @param baseURLString Base URL of the dynmap server
     * @throws IOException
     */
    public DynmapConnection(String baseURLString) throws IOException {
        if(!baseURLString.startsWith("http")) {
            baseURLString = "https://" + baseURLString;
        }
        if(baseURLString.endsWith("/")) {
            baseURLString = baseURLString.substring(0, baseURLString.length() - 1);
        }
        // Get the default world name
        try {
            dynmapConfiguration = HTTP.makeJSONHTTPRequest(
                    new URL(baseURLString + "/up/configuration"), DynmapConfiguration.class);
        } catch (UnknownHostException e) {
            throw new UnknownHostException("Unable to connect to Dynamp at " + baseURLString);
        }
        // Build the url
        queryURL = new URL(baseURLString + "/up/world/" + dynmapConfiguration.defaultworld + "/");
        this.baseURLString = baseURLString;

    }

    /**
     * Ask the server for a list of all player positions
     * 
     * @return Player positions
     * @throws IOException
     */
    public HashMap<String, PlayerPosition> getAllPlayerPositions() throws IOException {

        // Make request for all players
        DynmapUpdate update = null;
        try {
            update = HTTP.makeJSONHTTPRequest(queryURL, DynmapUpdate.class);
        } catch (IOException e) {
            for(DynmapConfiguration.World world : dynmapConfiguration.worlds) {
                queryURL = new URL(baseURLString + "/up/world/" + world.name + "/");
                try {
                    update = HTTP.makeJSONHTTPRequest(queryURL, DynmapUpdate.class);
                } catch (IOException ignored) {}
            }
        }

        if(update == null) {
            throw new IOException("Unable to connect to Dynamp at " + baseURLString + " (could not get data from any world)");
        }

        // Build a list of positions
        HashMap<String, PlayerPosition> positions = new HashMap<>(update.players.length);

        for (int i = 0; i < update.players.length; i++) {
            positions.put(update.players[i].name, new PlayerPosition(update.players[i].name, update.players[i].world, update.players[i].x, update.players[i].y, update.players[i].z));
        }

        return positions;
    }

}