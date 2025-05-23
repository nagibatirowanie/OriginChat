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
 import me.nagibatirowanie.originchat.utils.FormatUtil;
 import net.kyori.adventure.text.Component;
 import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
  * Module that displays chat messages as holograms above players' heads.
  */
 public class ChatBubblesModule extends AbstractModule implements Listener {
 
     private boolean enabled;
     private String format;
     private int maxLength;
     private int displayTime;
     private double bubbleHeight;
     private final Map<UUID, BukkitTask> activeBubbles;
     private final Map<UUID, String> activeHolograms;
     private List<String> allowedChats;
     
     private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .hexColors()
            // Using builder instead of legacySection to properly handle hex colors
            .build();
 
     /**
      * Constructs the ChatBubblesModule.
      *
      * @param plugin the main OriginChat plugin instance
      */
     public ChatBubblesModule(OriginChat plugin) {
         super(plugin, "chat_bubbles", "Chat Bubbles", "Displays chat messages as holograms above players' heads", "1.0");
         this.activeBubbles = new HashMap<>();
         this.activeHolograms = new HashMap<>();
         this.allowedChats = new ArrayList<>();
     }
 
     @Override
     public void onEnable() {
         loadModuleConfig("modules/chat_bubbles");
         loadConfig();
 
         if (!enabled) {
             return;
         }
 
         if (plugin.getServer().getPluginManager().getPlugin("DecentHolograms") == null) {
             log("DecentHolograms plugin not found. Chat Bubbles module will be disabled.");
             enabled = false;
             return;
         }
 
         plugin.getServer().getPluginManager().registerEvents(this, plugin);
         log("Chat Bubbles module enabled.");
     }
 
     @Override
     public void onDisable() {
         HandlerList.unregisterAll(this);
 
         activeHolograms.values().forEach(holoName -> {
             Hologram hologram = DHAPI.getHologram(holoName);
             if (hologram != null) {
                 DHAPI.removeHologram(holoName);
             }
         });
 
         activeBubbles.values().forEach(BukkitTask::cancel);
         activeBubbles.clear();
         activeHolograms.clear();
 
         log("Chat Bubbles module disabled.");
     }
 
     /**
      * Loads configuration settings for the module.
      */
     private void loadConfig() {
         try {
             enabled = config.getBoolean("enabled", true);
             format = config.getString("format", "{message}");
             maxLength = config.getInt("max-length", 32);
             displayTime = Math.max(config.getInt("display-time", 5), 1);
             bubbleHeight = config.getDouble("bubble-height", 2.5);
 
             allowedChats = config.getStringList("allow-chats");
             if (allowedChats == null) {
                 allowedChats = new ArrayList<>();
             }
 
             debug(String.format(
                 "Loaded configuration: enabled=%s, format=%s, maxLength=%d, displayTime=%d, bubbleHeight=%.2f, allowedChats=%s",
                 enabled, format, maxLength, displayTime, bubbleHeight, allowedChats
             ));
         } catch (Exception e) {
             log("Error loading Chat Bubbles configuration: " + e.getMessage());
             e.printStackTrace();
             enabled = false;
         }
     }
 
     /**
      * Checks whether a chat name is allowed for bubble display.
      *
      * @param chatName the chat channel name, may be null
      * @return true if allowed (or if allowedChats is empty)
      */
     private boolean isChatAllowed(String chatName) {
         return allowedChats.isEmpty() || chatName == null || allowedChats.contains(chatName);
     }
 
     /**
      * Listens to chat events and creates chat bubbles if enabled.
      *
      * @param event the asynchronous chat event
      */
     @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
     public void onPlayerChat(AsyncPlayerChatEvent event) {
         if (!enabled) {
             return;
         }
 
         Player player = event.getPlayer();
         String message = event.getMessage();
 
         if (allowedChats.isEmpty()) {
             plugin.getServer().getScheduler().runTask(plugin, () -> createChatBubble(player, message, null));
         }
     }
 
     /**
      * Creates and displays a chat bubble hologram above the player's head.
      *
      * @param player the player to display the bubble for
      * @param message the chat message
      * @param chatName the chat channel name, may be null
      */
     public void createChatBubble(Player player, String message, String chatName) {
         if (!isChatAllowed(chatName)) {
             debug("Chat bubble not created for chat: " + chatName);
             return;
         }
 
         UUID playerUuid = player.getUniqueId();
 
         if (activeBubbles.containsKey(playerUuid)) {
             activeBubbles.remove(playerUuid).cancel();
         }
         if (activeHolograms.containsKey(playerUuid)) {
             DHAPI.removeHologram(activeHolograms.remove(playerUuid));
         }
 
         // Format the message using FormatUtil with Component support
         String rawMessage = format.replace("{message}", message);
         String truncatedMessage = truncate(rawMessage, maxLength);
         
         // Create formatted component with placeholder support
         Component formattedComponent = FormatUtil.format(player, truncatedMessage);
         
         // Convert to legacy string for DecentHolograms compatibility
         String formattedText = LEGACY_SERIALIZER.serialize(formattedComponent);

         String holoName = "chat_bubble_" + playerUuid.toString().substring(0, 8);
         Location loc = player.getLocation().add(0, bubbleHeight, 0);

         Hologram existing = DHAPI.getHologram(holoName);
         if (existing != null) {
             DHAPI.removeHologram(holoName);
         }

         try {
             DHAPI.createHologram(holoName, loc, List.of(formattedText));
             activeHolograms.put(playerUuid, holoName);
         } catch (Exception e) {
             log("Error creating hologram: " + e.getMessage());
             return;
         }
 
         BukkitTask task = new BukkitRunnable() {
             private int ticksLeft = displayTime * 20;
 
             @Override
             public void run() {
                 if (!player.isOnline()) {
                     removeHologram(playerUuid);
                     cancel();
                     return;
                 }
 
                 Hologram holo = DHAPI.getHologram(holoName);
                 if (holo != null) {
                     DHAPI.moveHologram(holo, player.getLocation().add(0, bubbleHeight, 0));
                 }
 
                 if (--ticksLeft <= 0) {
                     removeHologram(playerUuid);
                     cancel();
                 }
             }
         }.runTaskTimer(plugin, 1L, 1L);
 
         activeBubbles.put(playerUuid, task);
     }
 
     /**
      * Removes the hologram and cancels its tracking task.
      *
      * @param playerUuid the player's UUID
      */
     private void removeHologram(UUID playerUuid) {
         String holoName = activeHolograms.remove(playerUuid);
         if (holoName != null) {
             Hologram holo = DHAPI.getHologram(holoName);
             if (holo != null) {
                 DHAPI.removeHologram(holoName);
             }
         }
         BukkitTask task = activeBubbles.remove(playerUuid);
         if (task != null) {
             task.cancel();
         }
     }
 
     /**
      * Truncates a string to the specified maximum length, appending "..." if truncated.
      *
      * @param input the original string
      * @param maxLen the maximum length
      * @return the truncated string
      */
     private String truncate(String input, int maxLen) {
         if (input.length() <= maxLen) {
             return input;
         }
         return input.substring(0, maxLen - 3) + "...";
     }
 }