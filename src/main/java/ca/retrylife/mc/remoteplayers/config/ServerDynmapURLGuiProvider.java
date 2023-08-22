package ca.retrylife.mc.remoteplayers.config;

import me.shedaniel.autoconfig.gui.registry.api.GuiProvider;
import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.autoconfig.util.Utils;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.StringListListEntry;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Based on https://github.com/dzwdz/chat_heads/blob/main/common/src/main/java/dzwdz/chat_heads/config/AliasesGuiProvider.java
public class ServerDynmapURLGuiProvider implements GuiProvider {
    // Match "(something.something[.something, etc]) -> (something.something[.something, etc])"
    public static final Pattern PATTERN = Pattern.compile("\\s*([^ >]+\\.[^ >]+)\\s+->\\s+([^ >]+\\.[^ >]+)\\s*");

    // Convert a list of "server IP -> Dynmap URL" strings to a map of server IPs to Dynmap URLs
    public static Map<String, String> toServerDynmapURLs(List<String> serverDynmapURLStrings) {
        Map<String, String> serverDynmapURLs = new LinkedHashMap<>();

        for (String s : serverDynmapURLStrings) {
            Matcher matcher = PATTERN.matcher(s);
            if (matcher.matches()) {
                String serverIP = matcher.group(1);
                String dynmapURL = matcher.group(2);
                serverDynmapURLs.put(serverIP, dynmapURL);
            } else throw new IllegalArgumentException();
        }

        return serverDynmapURLs;
    }

    public static List<String> toStrings(Map<String, String> serverDynmapURLs) {
        ArrayList<String> reverse = new ArrayList<>();

        for (var entry : serverDynmapURLs.entrySet()) {
            String serverIP = entry.getKey();
            String shopURL = entry.getValue();
            reverse.add(serverIP + " -> " + shopURL);
        }

        return reverse;

    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<AbstractConfigListEntry> get(String i13n, Field field, Object config, Object defaults, GuiRegistryAccess registry) {
        return Collections.singletonList(
                ConfigEntryBuilder.create()
                        .startStrList(Text.translatable(i13n), toStrings(Utils.getUnsafely(field, config)))
                        .setExpanded(true)
                        .setCreateNewInstance(entry -> new StringListListEntry.StringListCell("   ->   ", entry))
                        .setDefaultValue(() -> toStrings(Utils.getUnsafely(field, defaults)))
                        .setErrorSupplier(newValue -> {
                            try {
                                toServerDynmapURLs(newValue);
                                return Optional.empty();
                            } catch (Exception e) {
                                return Optional.of(Text.empty());
                            }
                        })
                        .setSaveConsumer(newValue -> Utils.setUnsafely(field, config, toServerDynmapURLs(newValue)))
                        .build()
        );
    }
}
