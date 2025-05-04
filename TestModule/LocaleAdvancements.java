package me.nagibatirowanie.originChat.Utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.nagibatirowanie.originLib.Messages.replaceHexColors;

public class LocaleAdvancements implements Listener {

    private final Plugin plugin;
    private boolean enabled;
    private boolean disableVanillaMessages;
    private String defaultFormat;
    private final Map<String, String> localeFormats = new HashMap<>();
    private final Set<String> excludedAdvancements = new HashSet<>();
    private String playerNameColor;
    private String titleColor;
    private String goalColor;

    public LocaleAdvancements(Plugin plugin) {
        this.plugin = plugin;
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            plugin.getLogger().info("Death messages module enabled");
            this.enabled = plugin.getConfig().getBoolean("death_messages.enable");
            loadConfig();
        }
    }

    private void loadConfig() {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("advancements");
        if (config == null) config = plugin.getConfig().createSection("advancements");

        disableVanillaMessages = config.getBoolean("disable-vanilla-messages", true);
        enabled = config.getBoolean("enabled", false);
        defaultFormat = config.getString("default_format", "");

        ConfigurationSection formats = config.getConfigurationSection("formats");
        if (formats != null) {
            for (String locale : formats.getKeys(false)) {
                localeFormats.put(locale.toLowerCase(Locale.ROOT), formats.getString(locale, defaultFormat));
            }
        }

        ConfigurationSection colors = config.getConfigurationSection("colors");
        if (colors != null) {
            playerNameColor = colors.getString("player_name", "#2f67e0");
            titleColor = colors.getString("title", "#1d52c4");
            goalColor = colors.getString("goal", "#2f67e0");
        }

        excludedAdvancements.clear();
        List<String> excluded = config.getStringList("excluded");
        for (String entry : excluded) {
            String[] parts = entry.split(":", 2);
            String namespace = "minecraft";
            String key = entry;
            if (parts.length == 2) {
                namespace = parts[0];
                key = parts[1];
            }
            key = key.replace('.', '/');
            NamespacedKey nsKey = new NamespacedKey(namespace, key);
            excludedAdvancements.add(nsKey.toString());
        }
        // Debug
        //plugin.getLogger().info("Loaded config:");
        //plugin.getLogger().info("enabled: " + enabled);
        //plugin.getLogger().info("disable-vanilla-messages: " + disableVanillaMessages);
    }

    @EventHandler
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        plugin.getLogger().info("PlayerAdvancementDoneEvent triggered.");


        if (disableVanillaMessages) {
            // Debug
            //plugin.getLogger().info("Vanilla messages are disabled.");
            event.message(null);
        }

        if (enabled) {
            Advancement advancement = event.getAdvancement();
            String advancementKey = advancement.getKey().toString();
            if (excludedAdvancements.contains(advancementKey)) {
                // Debug
                //plugin.getLogger().info("Advancement " + advancementKey + " is excluded.");
                return;
            }

            if (advancement.getDisplay() == null || !advancement.getDisplay().doesAnnounceToChat()) {
                // Debug
                //plugin.getLogger().info("Advancement " + advancementKey + " does not announce to chat.");
                return;
            }

            // Отправляем кастомное сообщение
            Player player = event.getPlayer();
            String locale = player.getLocale().toLowerCase(Locale.ROOT);
            String format = localeFormats.getOrDefault(locale, defaultFormat);
            if (format.isEmpty()) {
                // Debug
                // plugin.getLogger().info("Format for locale " + locale + " is empty.");
                return;
            }

            Component titleComponent = advancement.getDisplay().displayName();
            Component message = buildAdvancementMessage(format, player, titleComponent);
            player.sendMessage(message);
            // Debug
            //plugin.getLogger().info("Custom advancement message sent to " + player.getName());
        }else{
            if (disableVanillaMessages) {
                // Debug
                //plugin.getLogger().info("Vanilla messages are disabled.");
                event.message(null);
            }

        }
    }

    private Component buildAdvancementMessage(String format, Player player, Component titleComponent) {
        List<MessagePart> parts = parseMessage(format);
        Component messageComponent = Component.empty();

        for (MessagePart part : parts) {
            if (part instanceof TextPart) {
                String text = ((TextPart) part).text;
                String processed = processColors(text);
                Component comp = LegacyComponentSerializer.legacySection().deserialize(processed);
                messageComponent = messageComponent.append(comp);
            } else if (part instanceof PlaceholderPart) {
                String placeholder = ((PlaceholderPart) part).placeholder;
                Component comp = switch (placeholder) {
                    case "player" -> Component.text(player.getName()).color(parseColor(playerNameColor));
                    case "title" -> titleComponent.color(parseColor(titleColor));
                    case "goal" -> Component.text("goal").color(parseColor(goalColor));
                    default -> Component.text("{" + placeholder + "}");
                };
                messageComponent = messageComponent.append(comp);
            }
        }
        return messageComponent;
    }

    private TextColor parseColor(String color) {
        if (color.startsWith("#")) {
            return TextColor.fromHexString(color);
        } else {
            String hexColor = ChatColor.translateAlternateColorCodes('&', color);
            return TextColor.fromHexString(hexColor.replace("§", ""));
        }
    }

    private List<MessagePart> parseMessage(String message) {
        List<MessagePart> parts = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\{(player|title|goal)\\}").matcher(message);
        int lastIdx = 0;

        while (matcher.find()) {
            int start = matcher.start();
            if (start > lastIdx) {
                parts.add(new TextPart(message.substring(lastIdx, start)));
            }
            parts.add(new PlaceholderPart(matcher.group(1)));
            lastIdx = matcher.end();
        }
        if (lastIdx < message.length()) {
            parts.add(new TextPart(message.substring(lastIdx)));
        }
        return parts;
    }

    private String processColors(String text) {
        text = replaceHexColors(text);
        text = ChatColor.translateAlternateColorCodes('&', text);
        return text;
    }

    private static class MessagePart {}
    private static class TextPart extends MessagePart {
        final String text;
        TextPart(String text) { this.text = text; }
    }
    private static class PlaceholderPart extends MessagePart {
        final String placeholder;
        PlaceholderPart(String placeholder) { this.placeholder = placeholder; }
    }
}