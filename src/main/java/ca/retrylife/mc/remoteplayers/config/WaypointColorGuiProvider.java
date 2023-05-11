package ca.retrylife.mc.remoteplayers.config;

import me.shedaniel.autoconfig.gui.registry.api.GuiProvider;
import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

// Based on https://github.com/dzwdz/chat_heads/blob/main/common/src/main/java/dzwdz/chat_heads/config/SenderDetectionGuiProvider.java
public class WaypointColorGuiProvider implements GuiProvider {
    private static final String WAYPOINT_COLOR = "text.autoconfig.remoteplayers.option.waypointColor";
    
    private final Formatting[] colorFormatting = {
        Formatting.BLACK,
        Formatting.DARK_BLUE,
        Formatting.DARK_GREEN,
        Formatting.DARK_AQUA,
        Formatting.DARK_RED,
        Formatting.DARK_PURPLE,
        Formatting.GOLD,
        Formatting.GRAY,
        Formatting.DARK_GRAY,
        Formatting.BLUE,
        Formatting.GREEN,
        Formatting.AQUA,
        Formatting.RED,
        Formatting.LIGHT_PURPLE,
        Formatting.YELLOW,
        Formatting.WHITE
    };

    @SuppressWarnings({"rawtypes"})
    @Override
    public List<AbstractConfigListEntry> get(String i13n, Field field, Object config_, Object defaults, GuiRegistryAccess registry) {
        RemotePlayerConfig config = (RemotePlayerConfig) config_;
        if(config.waypointColor == null) config.waypointColor = RemotePlayerConfig.WaypointColor.DARK_PURPLE;

        return Collections.singletonList(
                ConfigEntryBuilder.create()
                        .startEnumSelector(Text.translatable(WAYPOINT_COLOR), RemotePlayerConfig.WaypointColor.class, config.waypointColor)
                        .setDefaultValue(RemotePlayerConfig.WaypointColor.DARK_PURPLE)
                        .setSaveConsumer(waypointColor -> config.waypointColor = waypointColor)
                        .setEnumNameProvider(anEnum -> Text.translatable(WAYPOINT_COLOR + "." + anEnum.name()).formatted(colorFormatting[anEnum.ordinal()]))
                        .build()
        );
    }

}