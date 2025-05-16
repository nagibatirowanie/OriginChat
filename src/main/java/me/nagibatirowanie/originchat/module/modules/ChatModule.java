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
import me.nagibatirowanie.originchat.locale.LocaleManager;
import me.nagibatirowanie.originchat.module.AbstractModule;
import me.nagibatirowanie.originchat.module.modules.ChatBubblesModule;
import me.nagibatirowanie.originchat.translate.TranslateManager;
import me.nagibatirowanie.originchat.utils.ColorUtil;
import me.nagibatirowanie.originchat.utils.TranslateUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A module for chat processing with multiple chat support, cooldowns and translation
 */
public class ChatModule extends AbstractModule implements Listener, CommandExecutor {

    private final Map<String, ChatConfig> chatConfigs = new HashMap<>();
    private boolean hexColors;
    private boolean miniMessage;
    private int maxMessageLength;
    private boolean enabled;
    
    /**
     * Возвращает карту конфигураций чатов
     * @return карта конфигураций чатов
     */
    public Map<String, ChatConfig> getChatConfigs() {
        return chatConfigs;
    }


    private LocaleManager localeManager;
    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    // Кеш для хранения кулдаунов игроков
    private final Map<UUID, Integer> playerCooldownCache = new ConcurrentHashMap<>();
    // Время последнего обновления кеша для каждого игрока
    private final Map<UUID, Long> cooldownCacheUpdateTime = new ConcurrentHashMap<>();
    private int defaultCooldown = 3;
    private boolean cooldownEnabled = true;
    // Время жизни кеша в миллисекундах (1 минута)
    private static final long CACHE_TTL = 60000;
    
    private boolean translationEnabled = true;
    
    // Ссылка на модуль чат-баблов для интеграции
    private ChatBubblesModule chatBubblesModule;

    private String msgNoPermission;
    private String msgNobodyHeard;
    private String msgChatNotFound;
    private String msgCooldown;
    private String msgTranslateEnabled;
    private String msgTranslateDisabled;

    public ChatModule(OriginChat plugin) {
        super(plugin, "chat", "Chat Module", "Adds chat distribution and formatting", "1.0");
    }

    @Override
    public void onEnable() {
        loadModuleConfig("modules/chat");
        if (config == null) {
            config = plugin.getConfigManager().getMainConfig();
        }
        
        loadConfig();
        if (!enabled) {
            return;
        }
        
        // Регистрируем обработчики событий и команд
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("translatetoggle").setExecutor(this);

        localeManager = plugin.getLocaleManager();
        
        // Получаем ссылку на модуль чат-баблов для интеграции
        try {
            chatBubblesModule = (ChatBubblesModule) plugin.getModuleManager().getModule("chat_bubbles");
            if (chatBubblesModule != null) {
                log("Интеграция с модулем Chat Bubbles успешно установлена");
            } else {
                log("Модуль Chat Bubbles не найден, интеграция невозможна");
            }
        } catch (Exception e) {
            log("❗ Ошибка при получении модуля Chat Bubbles: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Запускаем задачу очистки кеша кулдаунов каждую минуту
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::clearExpiredCooldownCache, 1200L, 1200L);
        
        // Инициализируем время последнего сообщения для всех онлайн-игроков
        for (Player player : Bukkit.getOnlinePlayers()) {
            lastMessageTime.put(player.getUniqueId(), 0L);
            plugin.getPluginLogger().info("[ChatModule] Инициализировано время последнего сообщения для игрока " + player.getName());
        }
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        // Очищаем кеш времени последних сообщений
        lastMessageTime.clear();
        plugin.getPluginLogger().info("[ChatModule] Кеш времени последних сообщений очищен");
    }

    /**
     * Load settings from the config
     */
    private void loadConfig() {
        try {
            enabled = config.getBoolean("enabled", true);
            hexColors = config.getBoolean("hex-colors", true);
            miniMessage = config.getBoolean("mini-message", true);
            maxMessageLength = config.getInt("max-message-length", 256);
            translationEnabled = config.getBoolean("translation.enabled", true);

            // Loading messages for players
            
            ConfigurationSection msgSection = config.getConfigurationSection("messages");
            if (msgSection != null) {
                msgNoPermission = msgSection.getString("no-permission", msgNoPermission);
                msgNobodyHeard = msgSection.getString("nobody-heard", msgNobodyHeard);
                msgChatNotFound = msgSection.getString("chat-not-found", msgChatNotFound);
                msgCooldown = msgSection.getString("cooldown", msgCooldown);
                msgTranslateDisabled = msgSection.getString("translate-disabled", msgTranslateDisabled);
                msgTranslateEnabled = msgSection.getString("translate-enabled", msgTranslateEnabled);
            }

            // Load Cooldown
            ConfigurationSection cooldownSection = config.getConfigurationSection("cooldown");
            if (cooldownSection != null) {
                if (cooldownSection.contains("enabled")) {
                    cooldownEnabled = cooldownSection.getBoolean("enabled", true);
                }
                if (cooldownSection.contains("default")) {
                    defaultCooldown = cooldownSection.getInt("default", 3);
                }
            } else {
                defaultCooldown = config.getInt("cooldown.default", 3);
                cooldownEnabled = true;
            }
            
            // Очищаем кеш кулдаунов при перезагрузке конфига
            playerCooldownCache.clear();
            cooldownCacheUpdateTime.clear();
            loadChatConfigs();
        } catch (Exception e) {
            log("❗ Error when loading chat configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }


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
     * Получает значение кулдауна для игрока на основе его прав
     * Ищет права формата originchat.chat.cooldown.<число> и возвращает наименьшее значение
     * Использует кеширование для оптимизации производительности
     * @param player игрок
     * @return значение кулдауна в секундах
     */
    private int getPlayerCooldown(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // Проверяем, есть ли кешированное значение и не устарело ли оно
        if (playerCooldownCache.containsKey(playerUUID)) {
            long lastUpdateTime = cooldownCacheUpdateTime.getOrDefault(playerUUID, 0L);
            if (System.currentTimeMillis() - lastUpdateTime < CACHE_TTL) {
                int cachedCooldown = playerCooldownCache.get(playerUUID);
                plugin.getPluginLogger().info("[ChatModule] Используем кешированное значение кулдауна для игрока " + player.getName() + ": " + cachedCooldown + " сек.");
                return cachedCooldown;
            }
        }
        
        // Если у игрока есть право администратора, возвращаем 0 (без кулдауна)
        if (player.hasPermission("originchat.admin")) {
            plugin.getPluginLogger().info("[ChatModule] Игрок " + player.getName() + " имеет право администратора, кулдаун отключен");
            updateCooldownCache(playerUUID, 0);
            return 0;
        }
        
        // Ищем все права формата originchat.chat.cooldown.<число>
        int minCooldown = defaultCooldown;
        for (int i = 0; i <= 60; i++) { // Проверяем значения от 0 до 60 секунд
            String permission = "originchat.chat.cooldown." + i;
            if (player.hasPermission(permission) && i < minCooldown) {
                minCooldown = i;
                plugin.getPluginLogger().info("[ChatModule] Найдено право " + permission + " для игрока " + player.getName() + ", установлен кулдаун: " + i + " сек.");
            }
        }
        
        // Обновляем кеш
        updateCooldownCache(playerUUID, minCooldown);
        plugin.getPluginLogger().info("[ChatModule] Установлен кулдаун для игрока " + player.getName() + ": " + minCooldown + " сек.");
        
        return minCooldown;
    }
    
    /**
     * Обновляет кеш кулдаунов для игрока
     * @param playerUUID UUID игрока
     * @param cooldownValue значение кулдауна
     */
    private void updateCooldownCache(UUID playerUUID, int cooldownValue) {
        playerCooldownCache.put(playerUUID, cooldownValue);
        cooldownCacheUpdateTime.put(playerUUID, System.currentTimeMillis());
    }
    
    /**
     * Очищает устаревшие записи в кеше кулдаунов
     */
    private void clearExpiredCooldownCache() {
        long currentTime = System.currentTimeMillis();
        Set<UUID> expiredEntries = new HashSet<>();
        
        for (Map.Entry<UUID, Long> entry : cooldownCacheUpdateTime.entrySet()) {
            if (currentTime - entry.getValue() > CACHE_TTL) {
                expiredEntries.add(entry.getKey());
            }
        }
        
        for (UUID uuid : expiredEntries) {
            playerCooldownCache.remove(uuid);
            cooldownCacheUpdateTime.remove(uuid);
        }
    }

    /**
     * Chat message handler
     */
    /**
     * Обработчик команды translatetoggle
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        //plugin.getPluginLogger().info("[ChatModule] Выполнение команды: " + command.getName());
        
        if (!(sender instanceof Player)) {
            //plugin.getPluginLogger().info("[ChatModule] Команда выполнена не игроком, отклоняем");
            sender.sendMessage("§cЭта команда доступна только для игроков");
            return true;
        }
        
        Player player = (Player) sender;
        //plugin.getPluginLogger().info("[ChatModule] Игрок " + player.getName() + " выполняет команду translatetoggle");
        
        if (!translationEnabled) {
            //plugin.getPluginLogger().info("[ChatModule] Функция автоперевода отключена на сервере");
            player.sendMessage(formatMessage("§cФункция автоперевода отключена на сервере."));
            return true;
        }
        
        //plugin.getPluginLogger().info("[ChatModule] Вызываем toggleTranslate для игрока " + player.getName());
        boolean newState = plugin.getTranslateManager().toggleTranslate(player);
        //plugin.getPluginLogger().info("[ChatModule] Новое состояние автоперевода для игрока " + player.getName() + ": " + newState);
        
        if (newState) {
            //plugin.getPluginLogger().info("[ChatModule] Отправляем сообщение о включении автоперевода игроку " + player.getName());
            localeManager.sendMessage(player, "commands.translate_enabled");
        } else {
            //plugin.getPluginLogger().info("[ChatModule] Отправляем сообщение о выключении автоперевода игроку " + player.getName());
            localeManager.sendMessage(player, "commands.translate_disabled");
        }
        
        return true;
    }
    
    /**
     * Проверяет, включен ли автоперевод для игрока
     * @param player игрок
     * @return true если автоперевод включен
     */
    public boolean isTranslateEnabled(Player player) {
        return plugin.getTranslateManager().isTranslateEnabled(player);
    }
    
    /**
     * Обработчик события входа игрока на сервер
     * Инициализирует время последнего сообщения
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) {
            return;
        }
        
        Player player = event.getPlayer();
        lastMessageTime.put(player.getUniqueId(), 0L);
        plugin.getPluginLogger().info("[ChatModule] Инициализировано время последнего сообщения для игрока " + player.getName());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!enabled) {
            return;
        }

        Player player = event.getPlayer();
        String message = event.getMessage();

        if (message.length() > maxMessageLength) {
            message = message.substring(0, maxMessageLength);
        }

        if (cooldownEnabled) {
            int cooldown = getPlayerCooldown(player);
            long now = System.currentTimeMillis();
            
            // Проверяем, есть ли игрок в кеше времени последнего сообщения
            if (!lastMessageTime.containsKey(player.getUniqueId())) {
                plugin.getPluginLogger().info("[ChatModule] Игрок " + player.getName() + " не найден в кеше времени последнего сообщения. Инициализируем.");
                lastMessageTime.put(player.getUniqueId(), 0L);
            }
            
            if (cooldown > 0) {
                long last = lastMessageTime.getOrDefault(player.getUniqueId(), 0L);
                long diff = (now - last) / 1000;
                plugin.getPluginLogger().info("[ChatModule] Проверка кулдауна для игрока " + player.getName() + ": прошло " + diff + " сек. из " + cooldown + " сек.");
                if (diff < cooldown) {
                    String msg = plugin.getConfigManager().getLocalizedMessage("chat", "messages.cooldown", player.getLocale().toString()).replace("{cooldown}", String.valueOf(cooldown - diff));
                    player.sendMessage(formatMessage(msg));
                    plugin.getPluginLogger().info("[ChatModule] Сообщение игрока " + player.getName() + " заблокировано из-за кулдауна. Осталось ждать: " + (cooldown - diff) + " сек.");
                    event.setCancelled(true);
                    return;
                }
            }
            
            // Всегда обновляем время последнего сообщения, даже если кулдаун равен 0
            plugin.getPluginLogger().info("[ChatModule] Обновляем время последнего сообщения для игрока " + player.getName());
            lastMessageTime.put(player.getUniqueId(), now);
        }

        event.setCancelled(true);

        for (Map.Entry<String, ChatConfig> entry : chatConfigs.entrySet()) {
            String chatName = entry.getKey();
            ChatConfig chatConfig = entry.getValue();

            if (message.startsWith(chatConfig.getPrefix())) {
                if (!chatConfig.getPrefix().isEmpty()) {
                    message = message.substring(chatConfig.getPrefix().length());
                }

                if (!chatConfig.getPermissionWrite().isEmpty() && !player.hasPermission(chatConfig.getPermissionWrite())) {
                    player.sendMessage(formatMessage(plugin.getConfigManager().getLocalizedMessage("chat", "messages.no-permission", player.getLocale().toString())));
                    return;
                }
                
                // Сохраняем финальное сообщение для использования в лямбдах
                final String finalMessage = message;
                String formattedMessage = formatChatMessage(player, finalMessage, chatConfig, chatName);
                
                // Создаем чат-бабл, если модуль доступен
                if (chatBubblesModule != null) {
                    // Вызываем метод createChatBubble в основном потоке с указанием имени чата
                    final String finalChatName = chatName;
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        chatBubblesModule.createChatBubble(player, finalMessage, finalChatName);
                        debug("Создан чат-бабл для сообщения игрока " + player.getName() + " в чате '" + finalChatName + "'");
                    });
                }
                
                // Если перевод отключен на сервере, отправляем сообщение всем как обычно
                if (!translationEnabled) {
                    sendMessageToPlayers(player, formattedMessage, chatConfig);
                    return;
                }
                
                // Отправляем сообщение игрокам с отключенным переводом
                List<Player> playersWithTranslation = new ArrayList<>();
                
                if (chatConfig.getRadius() > 0) {
                    // Для локального чата
                    boolean heard = false;
                    for (Player target : player.getWorld().getPlayers()) {
                        if (target.equals(player)) {
                            target.sendMessage(formattedMessage);
                            continue;
                        }

                        if (target.getLocation().distance(player.getLocation()) <= chatConfig.getRadius() &&
                                (chatConfig.getPermissionView().isEmpty() || target.hasPermission(chatConfig.getPermissionView()))) {
                            heard = true;
                            boolean targetTranslateEnabled = plugin.getTranslateManager().isTranslateEnabled(target);
                            plugin.getPluginLogger().info("[ChatModule] Игрок " + target.getName() + ", автоперевод: " + targetTranslateEnabled);
                            if (!targetTranslateEnabled) {
                                plugin.getPluginLogger().info("[ChatModule] Отправляем оригинальное сообщение игроку " + target.getName());
                                target.sendMessage(formattedMessage);
                            } else {
                                plugin.getPluginLogger().info("[ChatModule] Добавляем игрока " + target.getName() + " в список для перевода");
                                playersWithTranslation.add(target);
                            }
                        }
                    }
                    if (!heard) {
                        player.sendMessage(formatMessage(plugin.getConfigManager().getLocalizedMessage("chat", "messages.nobody-heard", player.getLocale().toString())));
                    }
                } else {
                    // Для глобального чата
                    for (Player target : Bukkit.getOnlinePlayers()) {
                        if (target.equals(player)) {
                            target.sendMessage(formattedMessage);
                            continue;
                        }
                        
                        if (chatConfig.getPermissionView().isEmpty() || target.hasPermission(chatConfig.getPermissionView())) {
                            boolean targetTranslateEnabled = plugin.getTranslateManager().isTranslateEnabled(target);
                            plugin.getPluginLogger().info("[ChatModule] Игрок " + target.getName() + ", автоперевод: " + targetTranslateEnabled);
                            if (!targetTranslateEnabled) {
                                plugin.getPluginLogger().info("[ChatModule] Отправляем оригинальное сообщение игроку " + target.getName());
                                target.sendMessage(formattedMessage);
                            } else {
                                plugin.getPluginLogger().info("[ChatModule] Добавляем игрока " + target.getName() + " в список для перевода");
                                playersWithTranslation.add(target);
                            }
                        }
                    }
                }
                
                // Если нет игроков с включенным переводом, завершаем обработку
                if (playersWithTranslation.isEmpty()) {
                    return;
                }
                
                // Собираем уникальные локали игроков
                Set<String> uniqueLocales = playersWithTranslation.stream()
                        .map(p -> plugin.getLocaleManager().getPlayerLocaleRaw(p))
                        .collect(Collectors.toSet());
                // Карта для хранения переведенных сообщений
                Map<String, String> translatedMessages = new ConcurrentHashMap<>();
                List<CompletableFuture<Void>> translationFutures = new ArrayList<>();
                String senderLocale = plugin.getLocaleManager().getPlayerLocaleRaw(player);
                // Запускаем асинхронный перевод для каждой локали
                for (String locale : uniqueLocales) {
                    // Пропускаем перевод только если локаль полностью совпадает с локалью отправителя
                    if (locale.equals(senderLocale)) {
                        translatedMessages.put(locale, finalMessage);
                        continue;
                    }
                    CompletableFuture<Void> future = TranslateUtil.translateAsync(finalMessage, locale)
                            .thenAccept(translatedMessage -> {
                                // Сохраняем переведенное сообщение
                                translatedMessages.put(locale, translatedMessage);
                            })
                            .exceptionally(ex -> {
                                plugin.getPluginLogger().warning("Ошибка при переводе сообщения на " + locale + ": " + ex.getMessage());
                                // В случае ошибки используем оригинальное сообщение
                                translatedMessages.put(locale, finalMessage);
                                return null;
                            });
                    translationFutures.add(future);
                }
                
                // Ожидаем завершения всех переводов
                CompletableFuture.allOf(translationFutures.toArray(new CompletableFuture[0]))
                        .thenRun(() -> {
                            // Отправляем переведенные сообщения игрокам
                            for (Player target : playersWithTranslation) {
                                String locale = plugin.getLocaleManager().getPlayerLocaleRaw(target);
                                String translatedMessage = translatedMessages.getOrDefault(locale, finalMessage);
                                String translatedFormattedMessage = formatChatMessage(player, translatedMessage, chatConfig, chatName);
                                target.sendMessage(translatedFormattedMessage);
                            }
                        });
                
                return;
            }
        }
        player.sendMessage(formatMessage(msgChatNotFound));
    }
    
    /**
     * Отправляет сообщение игрокам в соответствии с настройками чата
     */
    
    private void sendMessageToPlayers(Player sender, String formattedMessage, ChatConfig chatConfig) {
        if (chatConfig.getRadius() > 0) {
            boolean heard = false;
            for (Player target : sender.getWorld().getPlayers()) {
                if (target.equals(sender)) continue;

                if (target.getLocation().distance(sender.getLocation()) <= chatConfig.getRadius() &&
                        (chatConfig.getPermissionView().isEmpty() || target.hasPermission(chatConfig.getPermissionView()))) {
                    target.sendMessage(formattedMessage);
                    heard = true;
                }
            }
            if (!heard) {
                sender.sendMessage(formatMessage(msgNobodyHeard));
            }
            sender.sendMessage(formattedMessage);
        } else {
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> chatConfig.getPermissionView().isEmpty() || p.hasPermission(chatConfig.getPermissionView()))
                    .forEach(p -> p.sendMessage(formattedMessage));
        }
    }


    private String formatChatMessage(Player player, String message, ChatConfig config, String chatName) {
        // 1) Сырой формат из конфига с подстановкой неизменяемых плейсхолдеров
        String raw = config.getFormat()
            .replace("{player}", player.getName())
            .replace("{chat}", chatName)
            .replace("{world}", player.getWorld().getName());
    
        // 2) Маркер вместо {message}
        final String MSG_MARKER = "&&MSG&&";
        String withMarker = raw.replace("{message}", MSG_MARKER);
    
        // 3) Полное форматирование префикса и суффикса:
        //    – allowColors = true   → все теги цветов из конфига,
        //    – useMiniMessage = miniMessage,
        //    – allowPlaceholders = true → все %…% разворачиваются
        String allConfigFormatted = ColorUtil.format(
            player,
            withMarker,
            /* allowColors= */ true,
            /* useMiniMessage= */ miniMessage,
            /* allowPlaceholders= */ true
        );
    
        // 4) Разбиваем по маркеру, чтобы убрать его из итоговой строки
        String[] parts = allConfigFormatted.split(Pattern.quote(MSG_MARKER), -1);
        String prefix = parts.length > 0 ? parts[0] : "";
        String suffix = parts.length > 1 ? parts[1] : "";
    
        // 5) Проверяем права игрока на цвета и на плейсхолдеры
        boolean canColors       = player.hasPermission("originchat.format.colors");
        boolean canPlaceholders = player.hasPermission("originchat.format.placeholders");
    
        // 6) Форматируем текст сообщения игрока:
        //    – если есть право на цвета → разрешаем HEX и MiniMessage,
        //      и разворачиваем %…% только при наличии allowPlaceholders
        //    – если нет права на цвета → отключаем любые цвета/MiniMessage,
        //      но всё ещё можем разворачивать %…% при allowPlaceholders
        String msgFormatted = ColorUtil.format(
            player,
            message,
            /* allowColors=       */ canColors && hexColors,
            /* useMiniMessage=    */ canColors && miniMessage,
            /* allowPlaceholders= */ canPlaceholders
        );
    
        // 7) Склеиваем всё вместе и возвращаем
        return prefix + msgFormatted + suffix;
    }
    
    
    /**
     * Форматируем сообщение с учетом плейсхолдеров
     * Для системных сообщений всегда используем полное форматирование
     */
    private String formatMessage(String message) {
        return ColorUtil.format(message, true, true, true);
    }
    
    /**
     * Форматируем сообщение игроку с учетом плейсхолдеров и прав игрока
    */
    // private String formatMessage(Player player, String message) {
    //     // Для системных сообщений всегда разрешаем цвета и плейсхолдеры
    //     // Это не сообщения игрока, а сообщения системы игроку
    //     return ColorUtil.format(player, message, true, true, true);
    // }

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