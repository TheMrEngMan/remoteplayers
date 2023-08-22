package ca.retrylife.mc.remoteplayers.commands;

import ca.retrylife.mc.remoteplayers.Database;
import ca.retrylife.mc.remoteplayers.RemotePlayers;
import ca.retrylife.mc.remoteplayers.UpdateTask;
import ca.retrylife.mc.remoteplayers.dynmap.DynmapConnection;
import ca.retrylife.mc.remoteplayers.dynmap.PlayerPosition;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;

public class PlayerCommands {

    public static void register() {

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            LiteralCommandNode<FabricClientCommandSource> remotePlayersNode = ClientCommandManager
                    .literal("remoteplayers")
                    .executes(new RemotePlayersCommand())
                    .build();
            LiteralCommandNode<FabricClientCommandSource> listNode = ClientCommandManager
                    .literal("list")
                    .executes(new PlayerListCommand())
                    .build();
            LiteralCommandNode<FabricClientCommandSource> infoNode = ClientCommandManager
                    .literal("info")
                    .executes(new PlayerInfoCommand())
                    .build();
            ArgumentCommandNode<FabricClientCommandSource, String> playerNameNode = ClientCommandManager
                    .argument("name", StringArgumentType.word())
                    .suggests((context, builder) -> (context.getSource() != null && UpdateTask.playerPositions != null) ? CommandSource.suggestMatching(UpdateTask.playerPositions.keySet(), builder) : Suggestions.empty())
                    .executes(new PlayerInfoCommand())
                    .build();

            LiteralCommandNode<FabricClientCommandSource> playerListAliasNode = ClientCommandManager
                    .literal("rpl")
                    .executes(new PlayerListCommand())
                    .build();
            LiteralCommandNode<FabricClientCommandSource> playerInfoAliasNode = ClientCommandManager
                    .literal("rpi")
                    .executes(new PlayerInfoCommand())
                    .build();

            //usage: /rpl
            dispatcher.getRoot().addChild(playerListAliasNode);
            //usage: /rpi <player name>
            dispatcher.getRoot().addChild(playerInfoAliasNode);
            playerInfoAliasNode.addChild(playerNameNode);

            //usage: /remoteplayers list
            dispatcher.getRoot().addChild(remotePlayersNode);
            remotePlayersNode.addChild(listNode);
            //usage: /remoteplayers info <player name>
            remotePlayersNode.addChild(infoNode);
            infoNode.addChild(playerNameNode);
        });

    }
    static class RemotePlayersCommand implements Command<FabricClientCommandSource> {
        @Override
        public int run(CommandContext<FabricClientCommandSource> context) {
            context.getSource().sendFeedback(MutableText.of(Text.of("Usage:").getContent()).formatted(Formatting.RED));
            context.getSource().sendFeedback(MutableText.of(Text.of("/remoteplayers list").getContent()).formatted(Formatting.RED));
            context.getSource().sendFeedback(MutableText.of(Text.of("/remoteplayers info <player name>").getContent()).formatted(Formatting.RED));
            return -1;
        }
    }

    static class PlayerListCommand implements Command<FabricClientCommandSource> {
        @Override
        public int run(CommandContext<FabricClientCommandSource> context) {
            if(!RemotePlayers.enabled) {
                context.getSource().sendFeedback(Text.translatable("text.remoteplayers.chat.command.notenabled"));
                return -1;
            }

            if(UpdateTask.playerPositions == null || UpdateTask.playerPositions.isEmpty()) {
                context.getSource().sendFeedback(Text.translatable("text.remoteplayers.chat.prefix").append(Text.translatable("text.remoteplayers.chat.command.playerlist.noplayers")));
                return 1;
            }
            context.getSource().sendFeedback(Text.translatable("text.remoteplayers.chat.prefix").append(Text.of(String.format(Language.getInstance().get("text.remoteplayers.chat.command.playerlist.header"), UpdateTask.playerPositions.size()))));
            for(PlayerPosition playerPosition : UpdateTask.playerPositions.values()) {
                if(playerPosition.worldName.equals(DynmapConnection.BOGUS_WORLD_NAME)) {
                    context.getSource().sendFeedback(Text.translatable("text.remoteplayers.chat.prefix").append(Text.of(String.format(Language.getInstance().get("text.remoteplayers.chat.command.playerinfo.invisible").replaceAll("§<cc>", "§" + Integer.toHexString(Database.getInstance().waypointColor())), playerPosition.username))));
                } else {
                    context.getSource().sendFeedback(Text.translatable("text.remoteplayers.chat.prefix").append(Text.of(String.format(Language.getInstance().get("text.remoteplayers.chat.command.playerinfo.info").replaceAll("§<cc>", "§" + Integer.toHexString(Database.getInstance().waypointColor())), playerPosition.username, playerPosition.x, playerPosition.y, playerPosition.z, playerPosition.worldName))));
                }
            }
            return 1;
        }
    }

    static class PlayerInfoCommand implements Command<FabricClientCommandSource> {
        @Override
        public int run(CommandContext<FabricClientCommandSource> context) {
            if(!RemotePlayers.enabled) {
                context.getSource().sendFeedback(Text.translatable("text.remoteplayers.chat.command.notenabled"));
                return -1;
            }

            String playerName;
            try {
                playerName = StringArgumentType.getString(context, "name");
            } catch (IllegalArgumentException e) {
                context.getSource().sendFeedback(MutableText.of(Text.of("/remoteplayers info <player name>").getContent()).formatted(Formatting.RED));
                return -1;
            }

            if(UpdateTask.playerPositions == null || UpdateTask.playerPositions.isEmpty()) {
                context.getSource().sendFeedback(Text.translatable("text.remoteplayers.chat.prefix").append(Text.of(String.format(Language.getInstance().get("text.remoteplayers.chat.command.playerinfo.error").replaceAll("§<cc>", "§" + Integer.toHexString(Database.getInstance().waypointColor())), playerName))));
                return -1;
            }
            PlayerPosition playerPosition = UpdateTask.playerPositions.get(playerName);
            if(playerPosition != null) {
                if(playerPosition.worldName.equals(DynmapConnection.BOGUS_WORLD_NAME)) {
                    context.getSource().sendFeedback(Text.translatable("text.remoteplayers.chat.prefix").append(Text.of(String.format(Language.getInstance().get("text.remoteplayers.chat.command.playerinfo.invisible").replaceAll("§<cc>", "§" + Integer.toHexString(Database.getInstance().waypointColor())), playerPosition.username))));
                } else {
                    context.getSource().sendFeedback(Text.translatable("text.remoteplayers.chat.prefix").append(Text.of(String.format(Language.getInstance().get("text.remoteplayers.chat.command.playerinfo.info").replaceAll("§<cc>", "§" + Integer.toHexString(Database.getInstance().waypointColor())), playerPosition.username, playerPosition.x, playerPosition.y, playerPosition.z, playerPosition.worldName))));
                }
                return 1;
            }
            else {
                context.getSource().sendFeedback(Text.translatable("text.remoteplayers.chat.prefix").append(Text.of(String.format(Language.getInstance().get("text.remoteplayers.chat.command.playerinfo.error").replaceAll("§<cc>", "§" + Integer.toHexString(Database.getInstance().waypointColor())), playerName))));
                return -1;
            }

        }
    }

}
