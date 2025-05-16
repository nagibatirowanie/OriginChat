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

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.module.AbstractModule;
import me.nagibatirowanie.originchat.utils.ColorUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Module for displaying chat messages as holograms above players' heads
 */
public class ChatBubblesModule extends AbstractModule implements Listener {

    private boolean enabled;
    private String format;
    private int maxLength;
    private int displayTime;
    private double bubbleHeight; // Высота голограммы над головой игрока
    private final Map<UUID, BukkitTask> activeBubbles;
    private final Map<UUID, String> activeHolograms;
    private List<String> allowedChats; // Список разрешенных чатов для отображения чат-баблов

    public ChatBubblesModule(OriginChat plugin) {
        super(plugin, "chat_bubbles", "Chat Bubbles", "Displays chat messages as holograms above players' heads", "1.0");
        this.activeBubbles = new HashMap<>();
        this.activeHolograms = new HashMap<>();
        this.allowedChats = new ArrayList<>();
    }
    
    /**
     * Проверяет, разрешен ли указанный чат для отображения чат-баблов
     * @param chatName Название чата
     * @return true, если чат разрешен или список разрешенных чатов пуст
     */
    private boolean isChatAllowed(String chatName) {
        // Если список пуст, разрешены все чаты
        if (allowedChats.isEmpty()) {
            return true;
        }
        // Если chatName равен null, используем поведение по умолчанию (разрешено)
        if (chatName == null) {
            return true;
        }
        // Проверяем, есть ли чат в списке разрешенных
        return allowedChats.contains(chatName);
    }

    @Override
    public void onEnable() {
        loadModuleConfig("modules/chat_bubbles");
        loadConfig();
        
        if (!enabled) {
            return;
        }
        
        // Check if DecentHolograms plugin is installed
        if (plugin.getServer().getPluginManager().getPlugin("DecentHolograms") == null) {
            log("❗ DecentHolograms plugin not found. Chat Bubbles module will be disabled.");
            enabled = false;
            return;
        }
        
        // Register event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        log("Chat Bubbles module enabled");
    }

    @Override
    public void onDisable() {
        // Unregister event listener
        HandlerList.unregisterAll(this);
        
        // Remove all active holograms
        for (String holoName : activeHolograms.values()) {
            Hologram hologram = DHAPI.getHologram(holoName);
            if (hologram != null) {
                DHAPI.removeHologram(holoName);
            }
        }
        
        // Cancel all active tasks
        for (BukkitTask task : activeBubbles.values()) {
            task.cancel();
        }
        
        activeBubbles.clear();
        activeHolograms.clear();
        
        log("Chat Bubbles module disabled");
    }

    /**
     * Load settings from the config
     */
    private void loadConfig() {
        try {
            enabled = config.getBoolean("enabled", true);
            format = config.getString("format", "{message}");
            maxLength = config.getInt("max-length", 32);
            displayTime = config.getInt("display-time", 5);
            bubbleHeight = config.getDouble("bubble-height", 2.5); // Загружаем высоту голограммы из конфига
            
            // Загружаем список разрешенных чатов
            allowedChats = config.getStringList("allow-chats");
            if (allowedChats == null) {
                allowedChats = new ArrayList<>();
            }
            
            // Ensure display time is at least 1 second
            if (displayTime < 1) {
                displayTime = 1;
            }
            
            debug("Loaded configuration: enabled=" + enabled + ", format=" + format + ", maxLength=" + maxLength + ", displayTime=" + displayTime + ", bubbleHeight=" + bubbleHeight + ", allowedChats=" + allowedChats);
        } catch (Exception e) {
            log("❗ Error when loading chat bubbles configuration: " + e.getMessage());
            e.printStackTrace();
            enabled = false;
        }
    }

    /**
     * Chat message handler
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!enabled) {
            log("Чат-баблы отключены в конфигурации, сообщение игнорируется");
            return;
        }

        // Если список разрешенных чатов пуст, то чат-баблы отображаются для всех чатов
        // Обработка чатов происходит в ChatModule, который вызывает createChatBubble с указанием типа чата
        // Этот обработчик нужен только для совместимости с другими плагинами
        
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        log("Получено сообщение от игрока " + player.getName() + ": " + message);
        
        // Если список разрешенных чатов пуст, создаем чат-бабл для всех сообщений
        if (allowedChats.isEmpty()) {
            // Create chat bubble on the main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> createChatBubble(player, message, null));
        }
    }

    /**
     * Create a chat bubble above the player's head
     * @param player The player
     * @param message The message
     * @param chatName The name of the chat (can be null)
     */
    public void createChatBubble(Player player, String message, String chatName) {
        // Проверяем, разрешен ли этот тип чата для отображения чат-баблов
        if (!isChatAllowed(chatName)) {
            debug("Чат-бабл не будет создан для чата '" + chatName + "', так как он не в списке разрешенных чатов: " + allowedChats);
            return;
        }
        
        // Логируем информацию о чате
        if (chatName != null) {
            debug("Создание чат-бабла для сообщения в чате '" + chatName + "'");
        } else {
            debug("Создание чат-бабла для сообщения (чат не определен)");
        }
        UUID playerUuid = player.getUniqueId();
        log("Создание чат-бабла для игрока " + player.getName() + " (UUID: " + playerUuid + ")");
        
        // Cancel existing task if there is one
        if (activeBubbles.containsKey(playerUuid)) {
            log("Найдена активная задача для игрока, отменяем");
            activeBubbles.get(playerUuid).cancel();
            activeBubbles.remove(playerUuid);
        }
        
        // Remove existing hologram if there is one
        if (activeHolograms.containsKey(playerUuid)) {
            String holoName = activeHolograms.get(playerUuid);
            log("Найдена существующая голограмма: " + holoName + ", удаляем");
            Hologram hologram = DHAPI.getHologram(holoName);
            if (hologram != null) {
                DHAPI.removeHologram(holoName);
                log("Голограмма успешно удалена");
            } else {
                log("Голограмма не найдена в API DecentHolograms");
            }
            activeHolograms.remove(playerUuid);
        }
        
        // Format the message
        String formattedMessage = format.replace("{message}", message);
        log("Отформатированное сообщение: " + formattedMessage);
        
        // Truncate if too long
        if (formattedMessage.length() > maxLength) {
            formattedMessage = formattedMessage.substring(0, maxLength - 3) + "...";
            log("Сообщение обрезано до: " + formattedMessage);
        }
        
        // Apply colors
        formattedMessage = ColorUtil.format(formattedMessage);
        log("Сообщение с примененными цветами: " + formattedMessage);
        
        // Create the hologram
        String holoName = "chat_bubble_" + playerUuid.toString().substring(0, 8);
        Location playerLocation = player.getLocation();
        Location location = playerLocation.add(0, bubbleHeight, 0); // Используем настраиваемую высоту
        log("Создание голограммы на позиции: мир=" + location.getWorld().getName() + ", x=" + location.getX() + ", y=" + location.getY() + ", z=" + location.getZ() + ", высота=" + bubbleHeight);
        
        List<String> lines = new ArrayList<>();
        lines.add(formattedMessage);
        
        // Check if hologram with this name already exists
        Hologram existingHolo = DHAPI.getHologram(holoName);
        if (existingHolo != null) {
            log("Голограмма с именем " + holoName + " уже существует, удаляем");
            DHAPI.removeHologram(holoName);
        }
        
        // Create the hologram
        try {
            DHAPI.createHologram(holoName, location, lines);
            log("Голограмма успешно создана: " + holoName);
            activeHolograms.put(playerUuid, holoName);
        } catch (Exception e) {
            log("❗ Ошибка при создании голограммы: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
        // Schedule a task to follow the player and remove the hologram after the display time
        BukkitTask task = new BukkitRunnable() {
            private int ticksLeft = displayTime * 20; // Convert seconds to ticks
            
            @Override
            public void run() {
                // Check if player is still online
                if (!player.isOnline()) {
                    log("Игрок " + player.getName() + " вышел с сервера, удаляем голограмму");
                    removeHologram(playerUuid);
                    this.cancel();
                    return;
                }
                
                // Update hologram position
                Hologram hologram = DHAPI.getHologram(holoName);
                if (hologram != null) {
                    Location newLocation = player.getLocation().add(0, bubbleHeight, 0);
                    DHAPI.moveHologram(hologram, newLocation);
                    if (ticksLeft % 20 == 0) { // Логируем каждую секунду, а не каждый тик
                        debug("Обновлена позиция голограммы для игрока " + player.getName() + ": x=" + newLocation.getX() + ", y=" + newLocation.getY() + ", z=" + newLocation.getZ());
                    }
                } else {
                    log("❗ Голограмма не найдена при попытке обновления позиции");
                }
                
                // Decrease time left
                ticksLeft--;
                
                // Remove hologram if time is up
                if (ticksLeft <= 0) {
                    log("Время отображения голограммы истекло, удаляем");
                    removeHologram(playerUuid);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
        
        log("Задача отслеживания голограммы запущена, ID: " + task.getTaskId());
        activeBubbles.put(playerUuid, task);
    }
    
    /**
     * Remove a hologram
     * @param playerUuid The player UUID
     */
    private void removeHologram(UUID playerUuid) {
        log("Удаление голограммы для игрока с UUID: " + playerUuid);
        if (activeHolograms.containsKey(playerUuid)) {
            String holoName = activeHolograms.get(playerUuid);
            log("Найдена голограмма: " + holoName);
            Hologram hologram = DHAPI.getHologram(holoName);
            if (hologram != null) {
                DHAPI.removeHologram(holoName);
                log("Голограмма успешно удалена");
            } else {
                log("❗ Голограмма не найдена в API DecentHolograms при попытке удаления");
            }
            activeHolograms.remove(playerUuid);
        } else {
            log("Активная голограмма не найдена для игрока с UUID: " + playerUuid);
        }
        
        if (activeBubbles.containsKey(playerUuid)) {
            log("Удаление задачи отслеживания голограммы");
            activeBubbles.remove(playerUuid);
        }
    }
}