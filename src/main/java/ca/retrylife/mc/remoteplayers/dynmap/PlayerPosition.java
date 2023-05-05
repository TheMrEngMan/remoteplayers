package ca.retrylife.mc.remoteplayers.dynmap;

import com.mojang.authlib.GameProfile;

/**
 * A player's auth profile and position
 */
public class PlayerPosition {

    public GameProfile player;
    public String username;
    public String worldName;
    public final int x;
    public final int y;
    public final int z;

    public PlayerPosition(String username, String worldName, int x, int y, int z) {
        this.username = username;
        this.worldName = worldName;
        this.player = new GameProfile(null, username);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setPlayerGameProfile(GameProfile player) {
        this.player = player;
    }

}