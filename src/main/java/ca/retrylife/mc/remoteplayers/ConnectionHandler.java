package ca.retrylife.mc.remoteplayers;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import java.util.Objects;

public class ConnectionHandler {

    public static void init() {

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {

            if (!client.isIntegratedServerRunning()) {

                try {
                    RemotePlayers.currentServerIP = Objects.requireNonNull(client.getCurrentServerEntry()).address;
                    RemotePlayers.enabled = Database.getInstance().serverHasDynmapLinked(RemotePlayers.currentServerIP);
                } catch (NullPointerException e) {
                    RemotePlayers.enabled = false;
                }

            }
            else {
                RemotePlayers.enabled = false;
            }

        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, sender) -> RemotePlayers.onDisconnect());

    }

}
