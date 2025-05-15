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
// Используем ChatConfig из ChatModule

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
        super(plugin, "mentions", "Mentions Module", "Adds player mentions in chat with notifications", "1.2");
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

    private void loadConfig() {
        enabled            = config.getBoolean("enabled", true);
        mentionSymbol      = config.getString("mention_symbol", "@");
        mentionCooldown    = config.getInt("cooldown", 30);

        soundEnabled       = config.getBoolean("sound.enabled", true);
        String soundName   = config.getString("sound.name", "ENTITY_EXPERIENCE_ORB_PICKUP");
        try {
            mentionSound   = Sound.valueOf(soundName);
        } catch (IllegalArgumentException ex) {
            log("❗ Invalid sound name '" + soundName + "', using default.");
            mentionSound   = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
        }
        soundVolume        = (float) config.getDouble("sound.volume", 1.0);
        soundPitch         = (float) config.getDouble("sound.pitch", 1.0);

        titleEnabled       = config.getBoolean("title.enabled", true);
        titleFadeIn        = config.getInt("title.fade_in", 10);
        titleStay          = config.getInt("title.stay", 70);
        titleFadeOut       = config.getInt("title.fade_out", 20);
        titleDisplayMode   = config.getString("title.display_mode", "both").toLowerCase();
        if (!titleDisplayMode.matches("title|subtitle|actionbar|both")) {
            titleDisplayMode = "both";
        }

        chatNotificationEnabled = config.getBoolean("chat_notification.enabled", true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!enabled) return;

        Player sender  = event.getPlayer();
        String message = event.getMessage();
        
        // Определяем, в каком чате было отправлено сообщение
        ChatModule chatModule = (ChatModule) plugin.getModuleManager().getModule("chat");
        if (chatModule == null) {
            // Если модуль чата не найден, используем стандартную логику
            processStandardMentions(sender, message);
            return;
        }
        
        // Определяем конфигурацию чата по префиксу сообщения
        String chatName = "global";
        Object chatConfig = null;
        
        Map<String, ?> chatConfigs = chatModule.getChatConfigs();
        if (chatConfigs != null) {
            for (Map.Entry<String, ?> entry : chatConfigs.entrySet()) {
                Object config = entry.getValue();
                // Получаем значение prefix через рефлексию
                String prefix = "";
                try {
                    prefix = (String) config.getClass().getMethod("getPrefix").invoke(config);
                } catch (Exception e) {
                    plugin.getLogger().warning("[MentionsModule] Ошибка при получении префикса чата: " + e.getMessage());
                    continue;
                }
                
                if (!prefix.isEmpty() && message.startsWith(prefix)) {
                    chatName = entry.getKey();
                    chatConfig = config;
                    break;
                }
            }
            
            // Если не нашли по префиксу, используем глобальный чат
            if (chatConfig == null && chatConfigs.containsKey("global")) {
                chatConfig = chatConfigs.get("global");
            }
        }
        
        // Если не удалось определить конфигурацию чата, используем стандартную логику
        if (chatConfig == null) {
            processStandardMentions(sender, message);
            return;
        }
        
        // Ищем упоминания в сообщении
        Pattern pattern = Pattern.compile(Pattern.quote(mentionSymbol) + "(\\w+)");
        Matcher matcher = pattern.matcher(message);

        while (matcher.find()) {
            String name = matcher.group(1);
            Player mentioned = Bukkit.getPlayerExact(name);
            
            if (mentioned != null && mentioned.isOnline() && !mentioned.equals(sender)) {
                // Проверяем, есть ли у упомянутого игрока доступ к чату
                boolean hasAccess = true;
                
                try {
                    // Проверка прав на просмотр чата
                    String permissionView = (String) chatConfig.getClass().getMethod("getPermissionView").invoke(chatConfig);
                    if (!permissionView.isEmpty() && !mentioned.hasPermission(permissionView)) {
                        hasAccess = false;
                        plugin.getLogger().info("[MentionsModule] Игрок " + mentioned.getName() + 
                                              " не имеет прав на просмотр чата " + chatName);
                    }
                    
                    // Проверка радиуса для локального чата
                    if (hasAccess) {
                        int radius = (int) chatConfig.getClass().getMethod("getRadius").invoke(chatConfig);
                        if (radius > 0) {
                            double distance = mentioned.getLocation().distance(sender.getLocation());
                            if (distance > radius || !mentioned.getWorld().equals(sender.getWorld())) {
                                hasAccess = false;
                                plugin.getLogger().info("[MentionsModule] Игрок " + mentioned.getName() + 
                                                      " находится вне радиуса чата " + chatName + 
                                                      " (" + distance + " > " + radius + ")");
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[MentionsModule] Ошибка при проверке доступа к чату: " + e.getMessage());
                    // В случае ошибки разрешаем доступ
                    hasAccess = true;
                }
                
                // Если у игрока есть доступ к чату и нет кулдауна, отправляем уведомление
                if (hasAccess) {
                    if (isOnCooldown(mentioned.getName())) continue;
                    lastMentionTime.put(mentioned.getName(), System.currentTimeMillis());
                    Bukkit.getScheduler().runTask(plugin, () ->
                        sendMentionNotification(sender, mentioned)
                    );
                }
            }
        }
    }
    
    /**
     * Обрабатывает упоминания в стандартном режиме (без проверки доступа к чату)
     * Используется, когда модуль чата недоступен
     */
    private void processStandardMentions(Player sender, String message) {
        Pattern pattern = Pattern.compile(Pattern.quote(mentionSymbol) + "(\\w+)");
        Matcher matcher = pattern.matcher(message);

        while (matcher.find()) {
            String name = matcher.group(1);
            Player mentioned = Bukkit.getPlayerExact(name);
            if (mentioned != null && mentioned.isOnline() && !mentioned.equals(sender)) {
                if (isOnCooldown(mentioned.getName())) continue;
                lastMentionTime.put(mentioned.getName(), System.currentTimeMillis());
                Bukkit.getScheduler().runTask(plugin, () ->
                    sendMentionNotification(sender, mentioned)
                );
            }
        }
    }

    private boolean isOnCooldown(String playerName) {
        if (mentionCooldown <= 0) return false;
        long last = lastMentionTime.getOrDefault(playerName, 0L);
        long delta = (System.currentTimeMillis() - last) / 1000;
        return delta < mentionCooldown;
    }

    private void sendMentionNotification(Player sender, Player mentioned) {
        // Sound notification
        if (soundEnabled) {
            mentioned.playSound(mentioned.getLocation(), mentionSound, soundVolume, soundPitch);
        }

        // Title / Subtitle / ActionBar
        if (titleEnabled) {
            // общий заголовок
            String rawTitle      = plugin.getConfigManager()
                                         .getLocalizedMessage("mentions", "title", mentioned);
            // субтайтл, если нужен
            String rawSubtitle   = plugin.getConfigManager()
                                         .getLocalizedMessage("mentions", "subtitle", mentioned)
                                         .replace("{player}", sender.getName());
            // отдельный actionbar
            String rawActionBar  = plugin.getConfigManager()
                                         .getLocalizedMessage("mentions", "actionbar", mentioned)
                                         .replace("{player}", sender.getName());

            String formattedTitle    = ColorUtil.format(mentioned, rawTitle, true, true, true, true);
            String formattedSubtitle = ColorUtil.format(mentioned, rawSubtitle, true, true, true, true);
            String formattedAction   = ColorUtil.format(mentioned, rawActionBar, true, true, true, true);

            switch (titleDisplayMode) {
                case "title":
                    mentioned.sendTitle(formattedTitle, "", titleFadeIn, titleStay, titleFadeOut);
                    break;
                case "subtitle":
                    mentioned.sendTitle("", formattedSubtitle, titleFadeIn, titleStay, titleFadeOut);
                    break;
                case "actionbar":
                    mentioned.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        new TextComponent(formattedAction)
                    );
                    break;
                case "both":
                default:
                    mentioned.sendTitle(formattedTitle, formattedSubtitle, titleFadeIn, titleStay, titleFadeOut);
                    break;
            }
        }

        // Chat notification
        if (chatNotificationEnabled) {
            String chatMsg = plugin.getConfigManager()
                                   .getLocalizedMessage("mentions", "chat_message", mentioned)
                                   .replace("{player}", sender.getName());
            chatMsg = ColorUtil.format(mentioned, chatMsg, true, true, true, true);
            mentioned.sendMessage(chatMsg);
        }
    }
}
