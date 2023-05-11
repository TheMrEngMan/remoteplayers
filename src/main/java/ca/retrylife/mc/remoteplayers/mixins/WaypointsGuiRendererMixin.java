package ca.retrylife.mc.remoteplayers.mixins;

import ca.retrylife.mc.remoteplayers.Database;
import ca.retrylife.mc.remoteplayers.config.RemotePlayerConfig;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.minimap.waypoints.render.WaypointsGuiRenderer;
import xaero.common.settings.ModSettings;

@Mixin(WaypointsGuiRenderer.class)
public class WaypointsGuiRendererMixin {

    @Inject(method = "getOrder()I", at = @At("RETURN"), cancellable = true, remap = false)
    private void injected(CallbackInfoReturnable<Integer> cir) {
        RemotePlayerConfig.WaypointRenderBelowMode waypointRenderBelowMode = Database.getInstance().minimapWaypointsRenderBelow();
        boolean playerListDown = MinecraftClient.getInstance().options.playerListKey.isPressed() || ModSettings.keyAlternativeListPlayers.isPressed();
        switch (waypointRenderBelowMode) {
            case NEVER: return;
            case ALWAYS: cir.setReturnValue(-1); return;
            case WHEN_PLAYER_LIST_SHOWN: if(playerListDown) cir.setReturnValue(-1); return;
            case WHEN_PLAYER_LIST_HIDDEN: if(!playerListDown) cir.setReturnValue(-1);
        }
    }

}
