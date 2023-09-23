package ca.retrylife.mc.remoteplayers.mixins;

import ca.retrylife.mc.remoteplayers.Database;
import ca.retrylife.mc.remoteplayers.UpdateTask;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xaero.common.minimap.radar.MinimapRadar;
import ca.retrylife.mc.remoteplayers.dynmap.PlayerPosition;

import java.util.*;

@Mixin(MinimapRadar.class)
public class MinimapRadarMixin {

    @ModifyVariable(method = "updateRadar(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;Lxaero/common/minimap/MinimapProcessor;)V", at = @At("STORE"), ordinal = 0)
    private Iterable<Entity> updateRadarEntities(Iterable<Entity> worldEntities) {
        // Don't render if feature not enabled
        if(!Database.getInstance().entityRadarEnabled()) return worldEntities;
        // Don't render if there is no remote players available
        if(UpdateTask.playerPositions == null || UpdateTask.playerPositions.isEmpty()) return worldEntities;
        // Don't render if can't get access to world to check for players in range
        if(MinecraftClient.getInstance().world == null)  return worldEntities;

        List<AbstractClientPlayerEntity> playerClientEntityList = MinecraftClient.getInstance().world.getPlayers();
        ArrayList<String> renderedPlayerNames = new ArrayList<>();
        for (AbstractClientPlayerEntity playerClientEntity : playerClientEntityList) {
            renderedPlayerNames.add(playerClientEntity.getName().copyContentOnly().getString());
        }

        // For each remote player
        ArrayList<Entity> playerEntities = new ArrayList<>(UpdateTask.playerPositions.size());
        for (PlayerPosition playerPosition : UpdateTask.playerPositions.values()) {
            // Skip if player has invalid data
            if(playerPosition == null || playerPosition.player == null) continue;
            // Don't render if player is in different world
            if(!playerPosition.worldName.equals(UpdateTask.currentWorldMapName)) continue;
            // Don't render same player when they are actually in range
            if(renderedPlayerNames.contains(playerPosition.username)) continue;

            // Add remote player to list as an entity
            OtherClientPlayerEntity playerEntity = new OtherClientPlayerEntity(MinecraftClient.getInstance().world, playerPosition.player);
            playerEntity.refreshPositionAndAngles(playerPosition.x, playerPosition.y, playerPosition.z, 0, 0);
            playerEntities.add(playerEntity);
        }

        // Add all remote player entities to real entities in world
        ArrayList<Entity> worldEntitiesList = new ArrayList<>();
        worldEntities.forEach(worldEntitiesList::add);
        worldEntitiesList.addAll(playerEntities);
        return worldEntitiesList;
    }

}
