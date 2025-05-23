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
 import me.nagibatirowanie.originchat.utils.FormatUtil;
 import org.bukkit.configuration.ConfigurationSection;
 import org.bukkit.event.EventHandler;
 import org.bukkit.event.EventPriority;
 import org.bukkit.event.HandlerList;
 import org.bukkit.event.Listener;
 import org.bukkit.event.player.AsyncPlayerChatEvent;
 
 import java.util.HashMap;
 import java.util.Map;
 
 /**
  * Module that replaces textual emoticons with Unicode emoji characters in chat messages.
  */
 public class EmojiModule extends AbstractModule implements Listener {
 
     private boolean enabled;
     private final Map<String, String> emojiMap = new HashMap<>();
 
     public EmojiModule(OriginChat plugin) {
         super(plugin, "emoji", "Emoji Module", "Replaces textual emoticons with Unicode emoji characters", "1.0");
     }
 
     @Override
     public void onEnable() {
         loadModuleConfig("modules/emoji");
         loadConfig();
         if (!enabled) {
             return;
         }
 
         plugin.getServer().getPluginManager().registerEvents(this, plugin);
         log("Emoji Module enabled, loaded " + emojiMap.size() + " entries.");
     }
 
     @Override
     public void onDisable() {
         HandlerList.unregisterAll(this);
         emojiMap.clear();
     }
 
     /**
      * Loads configuration settings and populates the emoji map.
      */
     private void loadConfig() {
         try {
             enabled = config.getBoolean("enabled", true);
             emojiMap.clear();
 
             ConfigurationSection section = config.getConfigurationSection("emojis");
             if (section != null) {
                 for (String key : section.getKeys(false)) {
                     String value = section.getString(key);
                     if (value != null && !value.isEmpty()) {
                         emojiMap.put(key, value);
                     }
                 }
             }
         } catch (Exception e) {
             log("Error loading emoji configuration: " + e.getMessage());
             e.printStackTrace();
         }
     }
 
     /**
      * Replaces all configured textual emoticons in the message with Unicode emojis.
      *
      * @param message the original chat message
      * @return the modified message with emojis, or the original if no replacements were made
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
      * Handles the AsyncPlayerChatEvent to apply emoji replacements at the lowest priority.
      *
      * @param event the chat event
      */
     @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
     public void onPlayerChat(AsyncPlayerChatEvent event) {
         if (!enabled || emojiMap.isEmpty()) {
             return;
         }
 
         String original = event.getMessage();
         String modified = replaceEmojis(original);
         if (!modified.equals(original)) {
             event.setMessage(modified);
         }
     }
 }
 