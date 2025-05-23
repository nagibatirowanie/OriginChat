package me.nagibatirowanie.originchat.module.modules;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.module.AbstractModule;
import me.nagibatirowanie.originchat.utils.FormatUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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
 * Module to display localized advancement messages to players.
 */
public class LocaleAdvancementsModule extends AbstractModule implements Listener {

    private boolean disableVanillaMessages;
    private String defaultFormat;
    private final Map<String, String> localeFormats = new HashMap<>();
    private final Set<String> excludedAdvancements = new HashSet<>();
    private String playerNameColor;
    private String titleColor;
    private String goalColor;
    private boolean registered;

    public LocaleAdvancementsModule(OriginChat plugin) {
        super(plugin, "locale_advancements", "Locale Advancements", 
              "Displays localized advancement messages", "1.0");
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

    /**
     * Loads settings for advancements formatting and exclusions.
     */
    private void loadConfig() {
        try {
            ConfigurationSection advCfg = config.getConfigurationSection("advancements");
            if (advCfg == null) {
                advCfg = config.createSection("advancements");
            }

            disableVanillaMessages = advCfg.getBoolean("disable-vanilla-messages", true);
            defaultFormat = advCfg.getString("default_format", "");

            ConfigurationSection formats = advCfg.getConfigurationSection("formats");
            if (formats != null) {
                for (String locale : formats.getKeys(false)) {
                    localeFormats.put(locale.toLowerCase(Locale.ROOT),
                                      formats.getString(locale, defaultFormat));
                }
            }

            ConfigurationSection colors = advCfg.getConfigurationSection("colors");
            if (colors != null) {
                playerNameColor = colors.getString("player_name", "#2f67e0");
                titleColor = colors.getString("title", "#1d52c4");
                goalColor = colors.getString("goal", "#2f67e0");
            }

            excludedAdvancements.clear();
            for (String entry : advCfg.getStringList("excluded")) {
                String[] parts = entry.split(":", 2);
                String namespace = parts.length == 2 ? parts[0] : "minecraft";
                String key = parts.length == 2 ? parts[1] : parts[0];
                key = key.replace('.', '/');
                excludedAdvancements.add(new NamespacedKey(namespace, key).toString());
            }
        } catch (Exception e) {
            log("Error loading locale advancements config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Listens for player advancements and sends custom localized messages.
     *
     * @param event the advancement completion event
     */
    @EventHandler
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        if (disableVanillaMessages) {
            event.message(null);
        }

        Advancement adv = event.getAdvancement();
        String key = adv.getKey().toString();
        if (excludedAdvancements.contains(key)
            || adv.getDisplay() == null
            || !adv.getDisplay().doesAnnounceToChat()) {
            return;
        }

        Player achiever = event.getPlayer();
        Component titleComp = adv.getDisplay().displayName();

        for (Player recipient : Bukkit.getOnlinePlayers()) {
            String locale = recipient.getLocale().toLowerCase(Locale.ROOT);
            String format = localeFormats.getOrDefault(locale, defaultFormat);
            if (format.isEmpty()) continue;

            Component message = buildAdvancementMessage(format, achiever, titleComp);
            recipient.sendMessage(message);
        }
    }

    /**
     * Builds the advancement message component with placeholders replaced.
     *
     * @param format the message format containing placeholders {player}, {title}, {goal}
     * @param player the player who achieved the advancement
     * @param titleComponent the advancement title component
     * @return a serialized Component with colors and text
     */
    private Component buildAdvancementMessage(String format, Player player, Component titleComponent) {
        List<MessagePart> parts = parseMessage(format);
        Component result = Component.empty();

        for (MessagePart part : parts) {
            if (part instanceof TextPart) {
                String text = ((TextPart) part).text;
                Component comp = FormatUtil.format(text, true, true, true);
                result = result.append(comp);

            } else if (part instanceof PlaceholderPart) {
                String ph = ((PlaceholderPart) part).placeholder;
                Component comp;
                switch (ph) {
                    case "player": {
                        Component playerName = Component.text(player.getName());
                        Component coloredName = FormatUtil.format(playerNameColor)
                                .append(playerName);
                        comp = coloredName;
                        break;
                    }
                    case "title": {
                        Component coloredTitle = FormatUtil.format(titleColor)
                                .append(titleComponent);
                        comp = coloredTitle;
                        break;
                    }
                    case "goal": {
                        Component goalText = Component.text("goal");
                        Component coloredGoal = FormatUtil.format(goalColor)
                                .append(goalText);
                        comp = coloredGoal;
                        break;
                    }
                    default: comp = Component.text("{" + ph + "}");
                }
                result = result.append(comp);
            }
        }
        return result;
    }

    /**
     * Splits the format into text and placeholder parts.
     *
     * @param message the raw format string
     * @return a list of MessagePart elements
     */
    private List<MessagePart> parseMessage(String message) {
        List<MessagePart> parts = new ArrayList<>();
        Matcher m = Pattern.compile("\\{(player|title|goal)\\}").matcher(message);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                parts.add(new TextPart(message.substring(last, m.start())));
            }
            parts.add(new PlaceholderPart(m.group(1)));
            last = m.end();
        }
        if (last < message.length()) {
            parts.add(new TextPart(message.substring(last)));
        }
        return parts;
    }

    private static abstract class MessagePart {}
    private static class TextPart extends MessagePart {
        final String text;
        TextPart(String text) { this.text = text; }
    }
    private static class PlaceholderPart extends MessagePart {
        final String placeholder;
        PlaceholderPart(String placeholder) { this.placeholder = placeholder; }
    }
}
