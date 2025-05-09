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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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

    private final Map<UUID, Long> lastMessageTime = new HashMap<>();
    private final Map<String, Integer> cooldowns = new HashMap<>();
    private int defaultCooldown = 3;
    private boolean cooldownEnabled = true;
    
    private boolean translationEnabled = true;

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
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
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

            // Load Kuldown
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
                defaultCooldown = config.getInt("cooldown.default", 3);
                cooldowns.put("originchat.moder", config.getInt("cooldown.originchat.moder", 2));
                cooldowns.put("originchat.admin", config.getInt("cooldown.originchat.admin", 0));
            }
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
     * Chat message handler
     */
    /**
     * Обработчик команды translatetoggle
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.getPluginLogger().info("[ChatModule] Выполнение команды: " + command.getName());
        
        if (!(sender instanceof Player)) {
            plugin.getPluginLogger().info("[ChatModule] Команда выполнена не игроком, отклоняем");
            sender.sendMessage("§cЭта команда доступна только для игроков");
            return true;
        }
        
        Player player = (Player) sender;
        plugin.getPluginLogger().info("[ChatModule] Игрок " + player.getName() + " выполняет команду translatetoggle");
        
        if (!translationEnabled) {
            plugin.getPluginLogger().info("[ChatModule] Функция автоперевода отключена на сервере");
            player.sendMessage(formatMessage("§cФункция автоперевода отключена на сервере."));
            return true;
        }
        
        plugin.getPluginLogger().info("[ChatModule] Вызываем toggleTranslate для игрока " + player.getName());
        boolean newState = plugin.getTranslateManager().toggleTranslate(player);
        plugin.getPluginLogger().info("[ChatModule] Новое состояние автоперевода для игрока " + player.getName() + ": " + newState);
        
        if (newState) {
            plugin.getPluginLogger().info("[ChatModule] Отправляем сообщение о включении автоперевода игроку " + player.getName());
            player.sendMessage(formatMessage(msgTranslateEnabled));
        } else {
            plugin.getPluginLogger().info("[ChatModule] Отправляем сообщение о выключении автоперевода игроку " + player.getName());
            player.sendMessage(formatMessage(msgTranslateDisabled));
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
            if (cooldown > 0) {
                long now = System.currentTimeMillis();
                long last = lastMessageTime.getOrDefault(player.getUniqueId(), 0L);
                long diff = (now - last) / 1000;
                if (diff < cooldown) {
                    String msg = plugin.getConfigManager().getLocalizedMessage("chat", "messages.cooldown", player.getLocale().toString()).replace("{cooldown}", String.valueOf(cooldown - diff));
                    player.sendMessage(formatMessage(msg));
                    event.setCancelled(true);
                    return;
                }
                lastMessageTime.put(player.getUniqueId(), now);
            }
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
        String format = config.getFormat();
        
        // Заменяем основные плейсхолдеры
        format = format.replace("{player}", player.getName());
        format = format.replace("{message}", message);
        format = format.replace("{chat}", chatName);
        format = format.replace("{world}", player.getWorld().getName());
        
        // Используем метод format с передачей игрока для обработки PlaceholderAPI
        return ColorUtil.format(player, format);
    }
    
    /**
     * Форматируем сообщение с учетом плейсхолдеров
     */
    private String formatMessage(String message) {
        return ColorUtil.format(message);
    }
    
    /**
     * Форматируем сообщение игроку с учетом плейсхолдеров
     */
    private String formatMessage(Player player, String message) {
        return ColorUtil.format(player, message);
    }

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