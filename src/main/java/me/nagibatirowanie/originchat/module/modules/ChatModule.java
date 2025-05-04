package me.nagibatirowanie.originchat.module.modules;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.module.AbstractModule;
import me.nagibatirowanie.originchat.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Модуль для обработки чата с поддержкой нескольких чатов и кулдауном
 */
public class ChatModule extends AbstractModule implements Listener {

    private final Map<String, ChatConfig> chatConfigs = new HashMap<>();
    private boolean hexColors;
    private boolean miniMessage;
    private int maxMessageLength;
    private boolean enabled;

    // Кулдаун: карта <UUID, время последнего сообщения (мс)>
    private final Map<UUID, Long> lastMessageTime = new HashMap<>();
    // Настройки кулдауна: <permission, seconds>
    private final Map<String, Integer> cooldowns = new HashMap<>();
    private int defaultCooldown = 3;
    private boolean cooldownEnabled = true;

    // Сообщения для игроков
    private String msgNoPermission = "❗У вас нет прав для отправки сообщений в этот чат.";
    private String msgNobodyHeard = "❗Никто не услышал ваше сообщение.";
    private String msgChatNotFound = "❗Чат не найден. Используйте префикс для выбора чата.";
    private String msgCooldown = "⏳ Подождите {cooldown} секунд перед следующим сообщением!";

    public ChatModule(OriginChat plugin) {
        super(plugin, "chat", "Модуль чата", "Обрабатывает сообщения в чате с поддержкой нескольких чатов", "1.0");
    }

    @Override
    public void onEnable() {
        // Загрузка конфигурации
        loadModuleConfig("modules/chat");
        if (config == null) {
            config = plugin.getConfigManager().getMainConfig();
        }
        loadConfig();
        if (!enabled) {
            log("Модуль чата отключен в конфигурации. Пропускаем активацию.");
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        log("Модуль чата успешно загружен.");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        log("Модуль чата выключен.");
    }

    /**
     * Загрузить настройки из конфига
     */
    private void loadConfig() {
        try {
            enabled = config.getBoolean("enabled", true);
            hexColors = config.getBoolean("hex-colors", true);
            miniMessage = config.getBoolean("mini-message", true);
            maxMessageLength = config.getInt("max-message-length", 256);

            // Загрузка сообщений для игроков
            ConfigurationSection msgSection = config.getConfigurationSection("messages");
            if (msgSection != null) {
                msgNoPermission = msgSection.getString("no-permission", msgNoPermission);
                msgNobodyHeard = msgSection.getString("nobody-heard", msgNobodyHeard);
                msgChatNotFound = msgSection.getString("chat-not-found", msgChatNotFound);
                msgCooldown = msgSection.getString("cooldown", msgCooldown);
            }

            // Загрузка кулдауна
            ConfigurationSection cooldownSection = config.getConfigurationSection("cooldown");
            cooldowns.clear();
            if (cooldownSection != null) {
                for (String key : cooldownSection.getKeys(false)) {
                    if (key.equalsIgnoreCase("enabled")) {
                        cooldownEnabled = cooldownSection.getBoolean("enabled", true);
                    } else if (key.equalsIgnoreCase("default")) {
                        defaultCooldown = cooldownSection.getInt("default", 3);
                    } else {
                        cooldowns.put(key, cooldownSection.getInt(key, defaultCooldown));
                    }
                }
            } else {
                // Старый формат
                defaultCooldown = config.getInt("cooldown.default", 3);
                cooldowns.put("originchat.moder", config.getInt("cooldown.originchat.moder", 2));
                cooldowns.put("originchat.admin", config.getInt("cooldown.originchat.admin", 0));
            }
            // Загрузка конфигураций чатов
            loadChatConfigs();
        } catch (Exception e) {
            log("❗Ошибка при загрузке конфигурации чата: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Загрузить конфигурации чатов
     */
    private void loadChatConfigs() {
        chatConfigs.clear();
        ConfigurationSection chatsSection = config.getConfigurationSection("chats");
        if (chatsSection != null) {
            for (String chatName : chatsSection.getKeys(false)) {
                ConfigurationSection chatSection = chatsSection.getConfigurationSection(chatName);
                if (chatSection != null) {
                    ChatConfig chatConfig = new ChatConfig(
                            chatSection.getString("prefix", ""),
                            chatSection.getInt("radius", -1),
                            chatSection.getString("format", "#f0f0f0[{chat}] {player}: {message}"),
                            chatSection.getString("permission-write", ""),
                            chatSection.getString("permission-view", "")
                    );
                    chatConfigs.put(chatName, chatConfig);
                    debug("Загружен чат: " + chatName + ", префикс: " + chatConfig.getPrefix());
                }
            }
        }

        if (chatConfigs.isEmpty()) {
            ChatConfig defaultChat = new ChatConfig(
                    "", -1, "<gray>[{player}]</gray> <white>{message}</white>", "", ""
            );
            chatConfigs.put("global", defaultChat);
            debug("Добавлен чат по умолчанию, так как не найдено настроенных чатов.");
        }
    }

    /**
     * Получить кулдаун для игрока (в секундах)
     */
    private int getPlayerCooldown(Player player) {
        if (player.hasPermission("originchat.admin")) {
            return 0;
        }
        for (Map.Entry<String, Integer> entry : cooldowns.entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                return entry.getValue();
            }
        }
        return defaultCooldown;
    }

    /**
     * Обработчик сообщений в чате
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!enabled) {
            return;
        }

        Player player = event.getPlayer();
        String message = event.getMessage();

        // Проверка длины сообщения
        if (message.length() > maxMessageLength) {
            message = message.substring(0, maxMessageLength);
        }

        // Проверка кулдауна
        if (cooldownEnabled) {
            int cooldown = getPlayerCooldown(player);
            if (cooldown > 0) {
                long now = System.currentTimeMillis();
                long last = lastMessageTime.getOrDefault(player.getUniqueId(), 0L);
                long diff = (now - last) / 1000;
                if (diff < cooldown) {
                    String msg = msgCooldown.replace("{cooldown}", String.valueOf(cooldown - diff));
                    player.sendMessage(formatMessage(msg));
                    event.setCancelled(true);
                    return;
                }
                lastMessageTime.put(player.getUniqueId(), now);
            }
        }

        event.setCancelled(true); // Отменяем стандартное сообщение

        // Определяем, в какой чат отправляется сообщение
        for (Map.Entry<String, ChatConfig> entry : chatConfigs.entrySet()) {
            String chatName = entry.getKey();
            ChatConfig chatConfig = entry.getValue();

            if (message.startsWith(chatConfig.getPrefix())) {
                // Удаляем префикс из сообщения, если он есть
                if (!chatConfig.getPrefix().isEmpty()) {
                    message = message.substring(chatConfig.getPrefix().length());
                }

                // Проверяем права на отправку сообщений
                if (!chatConfig.getPermissionWrite().isEmpty() && !player.hasPermission(chatConfig.getPermissionWrite())) {
                    player.sendMessage(formatMessage(msgNoPermission));
                    return;
                }

                // Форматируем сообщение
                String formattedMessage = formatChatMessage(player, message, chatConfig, chatName);

                // Отправляем сообщение в зависимости от радиуса
                if (chatConfig.getRadius() > 0) {
                    boolean heard = false;
                    for (Player target : player.getWorld().getPlayers()) {
                        if (target.equals(player)) continue;

                        if (target.getLocation().distance(player.getLocation()) <= chatConfig.getRadius() &&
                                (chatConfig.getPermissionView().isEmpty() || target.hasPermission(chatConfig.getPermissionView()))) {
                            target.sendMessage(formattedMessage);
                            heard = true;
                        }
                    }
                    if (!heard) {
                        player.sendMessage(formatMessage(msgNobodyHeard));
                    }
                    player.sendMessage(formattedMessage);
                } else {
                    Bukkit.getOnlinePlayers().stream()
                            .filter(p -> chatConfig.getPermissionView().isEmpty() || p.hasPermission(chatConfig.getPermissionView()))
                            .forEach(p -> p.sendMessage(formattedMessage));
                }
                return;
            }
        }
        player.sendMessage(formatMessage(msgChatNotFound));
    }

    /**
     * Форматировать сообщение чата
     */
    private String formatChatMessage(Player player, String message, ChatConfig config, String chatName) {
        String format = config.getFormat();
        format = format.replace("{player}", player.getName());
        format = format.replace("{message}", message);
        format = format.replace("{chat}", chatName);
        format = format.replace("{world}", player.getWorld().getName());
        return formatMessage(format);
    }

    /**
     * Форматировать сообщение с применением цветов
     */
    private String formatMessage(String message) {
        return ColorUtil.format(message);
    }

    /**
     * Класс для хранения конфигурации чата
     */
    private static class ChatConfig {
        private final String prefix;
        private final int radius;
        private final String format;
        private final String permissionWrite;
        private final String permissionView;

        public ChatConfig(String prefix, int radius, String format, String permissionWrite, String permissionView) {
            this.prefix = prefix;
            this.radius = radius;
            this.format = format;
            this.permissionWrite = permissionWrite;
            this.permissionView = permissionView;
        }

        public String getPrefix() {
            return prefix;
        }

        public int getRadius() {
            return radius;
        }

        public String getFormat() {
            return format;
        }

        public String getPermissionWrite() {
            return permissionWrite;
        }

        public String getPermissionView() {
            return permissionView;
        }
    }
}