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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Модуль для замены текстовых эмодзи на UTF символы в сообщениях чата
 */

public class EmojiModule extends AbstractModule implements Listener {

    private boolean enabled;
    private final Map<String, String> emojiMap = new HashMap<>();

    public EmojiModule(OriginChat plugin) {
        super(plugin, "emoji", "Emoji Module", "Replaces text emoticons with UTF emoji symbols", "1.0");
    }

    @Override
    public void onEnable() {
        loadModuleConfig("modules/emoji");
        loadConfig();
        if (!enabled) {
            return;
        }

        // Регистрируем обработчик событий
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        log("The emoji module has been successfully enabled. Loading " + emojiMap.size() + " emoji.");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        emojiMap.clear();
        //log("Модуль эмодзи выключен.");
    }

    /**
     * Загружает настройки из конфигурации
     */
    private void loadConfig() {
        try {
            enabled = config.getBoolean("enabled", true);
            emojiMap.clear();

            // Загружаем эмодзи из конфигурации
            ConfigurationSection emojiSection = config.getConfigurationSection("emojis");
            if (emojiSection != null) {
                for (String key : emojiSection.getKeys(false)) {
                    String value = emojiSection.getString(key);
                    if (value != null && !value.isEmpty()) {
                        emojiMap.put(key, value);
                        //debug("Загружен эмодзи: " + key + " -> " + value);
                    }
                }
            }

            if (emojiMap.isEmpty()) {
                //log("Не найдено ни одного эмодзи в конфигурации. Модуль не будет работать.");
            }
        } catch (Exception e) {
            log("❗ Ошибка при загрузке конфигурации эмодзи: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Заменяет текстовые эмодзи на UTF символы в сообщении
     * @param message исходное сообщение
     * @return сообщение с замененными эмодзи
     */
    public String replaceEmojis(String message) {
        if (message == null || message.isEmpty() || emojiMap.isEmpty()) {
            return message;
        }

        String result = message;
        for (Map.Entry<String, String> entry : emojiMap.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Обработчик события отправки сообщения в чат
     * Приоритет LOWEST, чтобы обработать сообщение до других модулей
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!enabled || emojiMap.isEmpty()) {
            return;
        }

        String message = event.getMessage();
        String modifiedMessage = replaceEmojis(message);
        
        // Если сообщение было изменено, обновляем его
        if (!modifiedMessage.equals(message)) {
            event.setMessage(modifiedMessage);
            //debug("Заменены эмодзи в сообщении игрока " + event.getPlayer().getName() + ": " + message + " -> " + modifiedMessage);
        }
    }
}