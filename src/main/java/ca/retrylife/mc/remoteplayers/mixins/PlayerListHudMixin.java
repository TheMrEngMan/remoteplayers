package ca.retrylife.mc.remoteplayers.mixins;

import ca.retrylife.mc.remoteplayers.Database;
import ca.retrylife.mc.remoteplayers.UpdateTask;
import ca.retrylife.mc.remoteplayers.dynmap.PlayerPosition;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

    @Inject(at = @At("RETURN"), method = "getPlayerName", cancellable = true)
    public void getPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        if(UpdateTask.playerPositions == null || !Database.getInstance().showDimensionInTablist()) return;

        MutableText playerNameWithPrefix;
        PlayerPosition playerPosition = UpdateTask.playerPositions.get(entry.getProfile().getName());

        if (playerPosition == null) {
            playerNameWithPrefix = Text.translatable("text.remoteplayers.tab.unknown").copy();
        } else if (playerPosition.worldName.contains("_nether")) {
            playerNameWithPrefix = Text.translatable("text.remoteplayers.tab.nether").copy();
        } else if (playerPosition.worldName.contains("_the_end")) {
            playerNameWithPrefix = Text.translatable("text.remoteplayers.tab.end").copy();
        } else {
            playerNameWithPrefix = Text.translatable("text.remoteplayers.tab.overworld").copy();
        }

        Text displayName = entry.getDisplayName();
        MutableText playerName = null;
        try {
            playerName = cir.getReturnValue().copy();
        } catch (Exception ignored) {}
        Text nameToSet = displayName != null ? displayName : (playerName != null ? playerName : Text.literal(entry.getProfile().getName()));
        playerNameWithPrefix.append(nameToSet);
        cir.setReturnValue(playerNameWithPrefix);

    }

}