package me.nagibatirowanie.originchat.module.modules;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.module.AbstractModule;
import me.nagibatirowanie.originchat.utils.ColorUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Module that adds player mention support in chat with configurable notifications.
 */
public class MentionsModule extends AbstractModule implements Listener {

    private boolean enabled;
    private String mentionSymbol;
    private int mentionCooldown;

    private boolean soundEnabled;
    private Sound mentionSound;
    private float soundVolume;
    private float soundPitch;

    private boolean titleEnabled;
    private int titleFadeIn;
    private int titleStay;
    private int titleFadeOut;
    private String titleDisplayMode; // title | subtitle | actionbar | both

    private boolean chatNotificationEnabled;
    private final Map<String, Long> lastMentionTime = new HashMap<>();

    public MentionsModule(OriginChat plugin) {
        super(plugin, "mentions", "Mentions Module", 
              "Adds player mentions in chat with notifications", "1.2");
    }

    @Override
    public void onEnable() {
        loadModuleConfig("modules/mentions");
        if (config == null) {
            config = plugin.getConfigManager().getMainConfig();
        }
        loadConfig();
        if (!enabled) return;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        log("MentionsModule enabled. Symbol='" + mentionSymbol + "'");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        lastMentionTime.clear();
        log("MentionsModule disabled.");
    }

    /**
     * Loads mention settings from configuration.
     */
    private void loadConfig() {
        enabled = config.getBoolean("enabled", true);
        mentionSymbol = config.getString("mention_symbol", "@");
        mentionCooldown = config.getInt("cooldown", 30);

        soundEnabled = config.getBoolean("sound.enabled", true);
        String soundName = config.getString("sound.name", "ENTITY_EXPERIENCE_ORB_PICKUP");
        try {
            mentionSound = Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException ex) {
            log("Invalid sound name '" + soundName + "', using default.");
            mentionSound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
        }
        soundVolume = (float) config.getDouble("sound.volume", 1.0);
        soundPitch = (float) config.getDouble("sound.pitch", 1.0);

        titleEnabled = config.getBoolean("title.enabled", true);
        titleFadeIn = config.getInt("title.fade_in", 10);
        titleStay = config.getInt("title.stay", 70);
        titleFadeOut = config.getInt("title.fade_out", 20);
        titleDisplayMode = config.getString("title.display_mode", "both").toLowerCase();
        if (!titleDisplayMode.matches("title|subtitle|actionbar|both")) {
            titleDisplayMode = "both";
        }

        chatNotificationEnabled = config.getBoolean("chat_notification.enabled", true);
    }

    /**
     * Listens for player chat and processes mentions.
     *
     * @param event the async player chat event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!enabled) return;

        Player sender = event.getPlayer();
        String message = event.getMessage();

        processMentions(sender, message);
    }

    /**
     * Processes mentions in the message and sends notifications.
     *
     * @param sender  the player who sent the message
     * @param message the chat message
     */
    private void processMentions(Player sender, String message) {
        Pattern pattern = Pattern.compile(Pattern.quote(mentionSymbol) + "(\\w+)");
        Matcher matcher = pattern.matcher(message);

        while (matcher.find()) {
            String name = matcher.group(1);
            Player mentioned = Bukkit.getPlayerExact(name);
            if (mentioned == null || !mentioned.isOnline() || mentioned.equals(sender)) continue;

            if (isOnCooldown(mentioned.getName())) continue;
            lastMentionTime.put(mentioned.getName(), System.currentTimeMillis());
            Bukkit.getScheduler().runTask(plugin, () -> sendMentionNotification(sender, mentioned));
        }
    }

    /**
     * Checks if the player is still on mention cooldown.
     *
     * @param playerName the name of the mentioned player
     * @return true if on cooldown, false otherwise
     */
    private boolean isOnCooldown(String playerName) {
        if (mentionCooldown <= 0) return false;
        long last = lastMentionTime.getOrDefault(playerName, 0L);
        long elapsed = (System.currentTimeMillis() - last) / 1000;
        return elapsed < mentionCooldown;
    }

    /**
     * Sends mention notifications (sound, title, chat) to the mentioned player.
     *
     * @param sender    the player who mentioned
     * @param mentioned the player being mentioned
     */
    private void sendMentionNotification(Player sender, Player mentioned) {
        if (soundEnabled) {
            mentioned.playSound(mentioned.getLocation(), mentionSound, soundVolume, soundPitch);
        }

        if (titleEnabled) {
            String rawTitle = plugin.getConfigManager()
                                    .getLocalizedMessage("mentions", "title", mentioned);
            String rawSubtitle = plugin.getConfigManager()
                                        .getLocalizedMessage("mentions", "subtitle", mentioned)
                                        .replace("{player}", sender.getName());
            String rawActionBar = plugin.getConfigManager()
                                        .getLocalizedMessage("mentions", "actionbar", mentioned)
                                        .replace("{player}", sender.getName());

            String title = ColorUtil.format(mentioned, rawTitle, true, true, true, true);
            String subtitle = ColorUtil.format(mentioned, rawSubtitle, true, true, true, true);
            String actionBar = ColorUtil.format(mentioned, rawActionBar, true, true, true, true);

            switch (titleDisplayMode) {
                case "title":
                    mentioned.sendTitle(title, "", titleFadeIn, titleStay, titleFadeOut);
                    break;
                case "subtitle":
                    mentioned.sendTitle("", subtitle, titleFadeIn, titleStay, titleFadeOut);
                    break;
                case "actionbar":
                    mentioned.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBar));
                    break;
                default:
                    mentioned.sendTitle(title, subtitle, titleFadeIn, titleStay, titleFadeOut);
            }
        }

        if (chatNotificationEnabled) {
            String rawChat = plugin.getConfigManager()
                                    .getLocalizedMessage("mentions", "chat_message", mentioned)
                                    .replace("{player}", sender.getName());
            String chatMsg = ColorUtil.format(mentioned, rawChat, true, true, true, true);
            mentioned.sendMessage(chatMsg);
        }
    }
}
