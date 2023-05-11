package ca.retrylife.mc.remoteplayers.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.minecraft.text.Text;

import java.util.LinkedHashMap;
import java.util.Map;

@Config(name = "remoteplayers")
public class RemotePlayerConfig implements ConfigData {

    public boolean modEnabled = true;

    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Gui.Tooltip()
    public boolean entityRadarEnabled = true;

    @ConfigEntry.Gui.Tooltip()
    public boolean inGameWaypointsEnabled = true;

    @ConfigEntry.Gui.Tooltip()
    public boolean showOverworldPositionInNether = true;
    @ConfigEntry.Gui.Tooltip()
    public boolean showNetherPositionInOverworld = true;

    @ConfigEntry.Gui.Tooltip()
    @ConfigEntry.BoundedDiscrete(max = 100, min = 0)
    public int minimumWaypointDistance = 25;
    @ConfigEntry.Gui.Tooltip()
    @ConfigEntry.BoundedDiscrete(max = 100, min = 0)
    public int minimumVisibleWaypointDistance = 50;

    // Dark purple by default as it is less likely the waypoints
    // will blend into natural terrain, while not being too bright
    @ConfigEntry.Gui.Tooltip()
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public WaypointColor waypointColor = WaypointColor.DARK_PURPLE;

    @ConfigEntry.Gui.Tooltip()
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public WaypointRenderBelowMode minimapWaypointsRenderBelow = WaypointRenderBelowMode.NEVER;

    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Gui.Tooltip()
    @ConfigEntry.BoundedDiscrete(max = 60, min = 1)
    public int updateInterval = 10;

    @ConfigEntry.Gui.Tooltip()
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public ChatNotificationType chatNotificationType = ChatNotificationType.NONE;

    @ConfigEntry.Gui.Tooltip()
    public Map<String, String> serverDynmapURLs = new LinkedHashMap<>(); // server IP -> dynmap URL

    public enum WaypointRenderBelowMode {
        NEVER,
        WHEN_PLAYER_LIST_SHOWN,
        WHEN_PLAYER_LIST_HIDDEN,
        ALWAYS;

        @Override
        public String toString() {
            return Text.translatable("text.autoconfig.remoteplayers.option.minimapWaypointsRenderBelow." + this.name()).getString();
        }
    }

    public enum ChatNotificationType {
        NONE,
        CHAT,
        ACTION_BAR,
        BOTH;

        @Override
        public String toString() {
            return Text.translatable("text.autoconfig.remoteplayers.option.chatNotificationType." + this.name()).getString();
        }
    }

    public enum WaypointColor {
        BLACK,
        DARK_BLUE,
        DARK_GREEN,
        DARK_AQUA,
        DARK_RED,
        DARK_PURPLE,
        GOLD,
        GRAY,
        DARK_GRAY,
        BLUE,
        GREEN,
        AQUA,
        RED,
        LIGHT_PURPLE,
        YELLOW,
        WHITE
    }

}