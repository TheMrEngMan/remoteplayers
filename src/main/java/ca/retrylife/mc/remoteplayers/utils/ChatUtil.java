package ca.retrylife.mc.remoteplayers.utils;

import ca.retrylife.mc.remoteplayers.Database;
import ca.retrylife.mc.remoteplayers.config.RemotePlayerConfig.ChatNotificationType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

public class ChatUtil {
    public static void showErrorChatMessage(ClientPlayerEntity player, Exception e, String prefixText, String helpText) {
        Text errorsText = Text.of("§c" + e.getClass().getName() + ": " + e.getMessage());
        if(player != null) player.sendMessage(Text.translatable("text.remoteplayers.chat.prefix").append("§4" + prefixText + "\n").append(errorsText).append("\n§4" + helpText));
    }

    public static void showNotificationChatMessage(ClientPlayerEntity player, Text text) {
        if(player == null) return;
        ChatNotificationType chatNotificationType = Database.getInstance().getChatNotificationType();
        if(chatNotificationType == ChatNotificationType.CHAT || chatNotificationType == ChatNotificationType.BOTH) {
            player.sendMessage(Text.translatable("text.remoteplayers.chat.prefix").append(text));
        }
        if(chatNotificationType == ChatNotificationType.ACTION_BAR || chatNotificationType == ChatNotificationType.BOTH) {
            player.sendMessage(Text.translatable("text.remoteplayers.chat.prefix").append(text), true);
        }
    }
}
