package ca.retrylife.mc.remoteplayers.utils;

import ca.retrylife.mc.remoteplayers.Database;
import ca.retrylife.mc.remoteplayers.dynmap.PlayerPosition;
import xaero.common.minimap.waypoints.Waypoint;

import java.util.regex.Pattern;

/**
 * A wrapper to improve creating temp waypoints for players
 */
public class PlayerWaypoint extends Waypoint {

    public static final Pattern usernameValidCharacters = Pattern.compile("[0-9a-zA-Z_]");

    public PlayerWaypoint(PlayerPosition player, String suffix) {
        this(player.x, player.y, player.z, player.username, suffix);
    }

    public PlayerWaypoint(int x, int y, int z, String name, String suffix) {
        super(x, y, z, name + suffix, ".", Database.getInstance().waypointColor(), 0, true);
        // Check if the player's username starts with a valid character
        String firstChar = String.valueOf(name.charAt(0));
        if(!usernameValidCharacters.matcher(firstChar).matches()) {
            // If not, this is most likely a Bedrock player connecting to a Java server
            // In this case, a prefix is applied to the username,
            // so the real first character comes after the prefix
            firstChar = String.valueOf(name.charAt(1));
        }
        super.setSymbol(firstChar);
    }

}