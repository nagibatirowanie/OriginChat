/*
 * This file is part of OriginChat, a Minecraft plugin.
 *
 * Copyright (c) 2025 nagibatirowanie
 *
 * OriginChat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this plugin. If not, see <https://www.gnu.org/licenses/>.
 *
 * Created with ❤️ for the Minecraft community.
 */

package me.nagibatirowanie.originchat.module.modules;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.module.AbstractModule;
import me.nagibatirowanie.originchat.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Module for localized achievement messages
 */
public class LocaleAdvancementsModule extends AbstractModule implements Listener {

    private boolean disableVanillaMessages;
    private String defaultFormat;
    private final Map<String, String> localeFormats = new HashMap<>();
    private final Set<String> excludedAdvancements = new HashSet<>();
    private String playerNameColor;
    private String titleColor;
    private String goalColor;
    private boolean registered = false;

    public LocaleAdvancementsModule(OriginChat plugin) {
        super(plugin, "locale_advancements", "Locale Advancements", 
              "Module for displaying localized achievement messages", "1.0");
    }

    @Override
    public void onEnable() {
        loadModuleConfig("modules/locale_advancements");
        if (config == null) {
            config = plugin.getConfigManager().getMainConfig();
        }
        loadConfig();
        
        if (!registered) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
    }

    @Override
    public void onDisable() {
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
    }

    private void loadConfig() {
        try {
            ConfigurationSection advancementsConfig = config.getConfigurationSection("advancements");
            if (advancementsConfig == null) advancementsConfig = config.createSection("advancements");

            disableVanillaMessages = advancementsConfig.getBoolean("disable-vanilla-messages", true);
            defaultFormat = advancementsConfig.getString("default_format", "");

            ConfigurationSection formats = advancementsConfig.getConfigurationSection("formats");
            if (formats != null) {
                for (String locale : formats.getKeys(false)) {
                    localeFormats.put(locale.toLowerCase(Locale.ROOT), formats.getString(locale, defaultFormat));
                }
            }

            ConfigurationSection colors = advancementsConfig.getConfigurationSection("colors");
            if (colors != null) {
                playerNameColor = colors.getString("player_name", "#2f67e0");
                titleColor = colors.getString("title", "#1d52c4");
                goalColor = colors.getString("goal", "#2f67e0");
            }

            excludedAdvancements.clear();
            List<String> excluded = advancementsConfig.getStringList("excluded");
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
        
        debug("The configuration of the localized achievements module has been uploaded.");
    } catch (Exception e) {
        log("❗ Error when loading LocalAdvancments configuration: " + e.getMessage());
        e.printStackTrace();
    }
    }

    @EventHandler
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        debug("The PlayerAdvancementDoneEvent event has been triggered.");

        if (disableVanillaMessages) {
            event.message(null);
        }

        Advancement advancement = event.getAdvancement();
        String advancementKey = advancement.getKey().toString();
        if (excludedAdvancements.contains(advancementKey)) {
            debug("Advancement " + advancementKey + " excluded.");
            return;
        }

        if (advancement.getDisplay() == null || !advancement.getDisplay().doesAnnounceToChat()) {
            debug("Advancement " + advancementKey + " is not announced in the chat.");
            return;
        }

        // Отправляем кастомное сообщение
        Player player = event.getPlayer();
        String locale = player.getLocale().toLowerCase(Locale.ROOT);
        String format = localeFormats.getOrDefault(locale, defaultFormat);
        if (format.isEmpty()) {
            debug("Format for locale " + locale + " empty.");
            return;
        }

        Component titleComponent = advancement.getDisplay().displayName();
        Component message = buildAdvancementMessage(format, player, titleComponent);
        player.sendMessage(message);
        debug("Custom achievement message sent to the player " + player.getName());
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
        text = ColorUtil.format(text);
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