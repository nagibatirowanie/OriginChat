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
 import net.kyori.adventure.text.Component;
 import org.bukkit.Bukkit;
 import org.bukkit.Sound;
 import org.bukkit.entity.Player;
 import org.bukkit.event.EventHandler;
 import org.bukkit.event.EventPriority;
 import org.bukkit.event.HandlerList;
 import org.bukkit.event.Listener;
 import org.bukkit.event.player.PlayerBedEnterEvent;
 import org.bukkit.event.player.PlayerBedLeaveEvent;
 
 /**
  * Module that customizes messages and sounds for bed interactions.
  */
 public class BedMessagesModule extends AbstractModule implements Listener {
 
     private boolean enabled;
     private boolean playSound;
     private String successSoundName;
     private float successSoundVolume;
     private float successSoundPitch;
     private String errorSoundName;
     private float errorSoundVolume;
     private float errorSoundPitch;
     private String leaveSoundName;
     private float leaveSoundVolume;
     private float leaveSoundPitch;
 
     /**
      * Constructs the BedMessagesModule.
      *
      * @param plugin the main OriginChat plugin instance
      */
     public BedMessagesModule(OriginChat plugin) {
         super(plugin, "bed_messages", "Bed Messages", "Customizes messages when players interact with beds", "1.0");
     }
 
     @Override
     public void onEnable() {
         loadModuleConfig("modules/bed_messages");
         loadConfig();
         
         if (!enabled) {
             return;
         }
         
         Bukkit.getPluginManager().registerEvents(this, plugin);
         log("Bed messages module loaded.");
     }
 
     @Override
     public void onDisable() {
         HandlerList.unregisterAll(this);
         log("Bed messages module disabled.");
     }
 
     /**
      * Loads configuration settings for the module.
      */
     protected void loadConfig() {
         try {
             enabled = config.getBoolean("enabled", true);
             playSound = config.getBoolean("play_sound", true);
 
             successSoundName = config.getString("sounds.success.name", "BLOCK_NOTE_BLOCK_PLING");
             successSoundVolume = (float) config.getDouble("sounds.success.volume", 1.0);
             successSoundPitch = (float) config.getDouble("sounds.success.pitch", 1.0);
 
             errorSoundName = config.getString("sounds.error.name", "BLOCK_NOTE_BLOCK_BASS");
             errorSoundVolume = (float) config.getDouble("sounds.error.volume", 1.0);
             errorSoundPitch = (float) config.getDouble("sounds.error.pitch", 0.8);
 
             leaveSoundName = config.getString("sounds.leave.name", "BLOCK_NOTE_BLOCK_HARP");
             leaveSoundVolume = (float) config.getDouble("sounds.leave.volume", 1.0);
             leaveSoundPitch = (float) config.getDouble("sounds.leave.pitch", 1.2);
 
             saveModuleConfig("modules/bed_messages");
             
             debug(String.format(
                 "Loaded configuration: enabled=%s, playSound=%s",
                 enabled, playSound
             ));
         } catch (Exception e) {
             log("Error loading BedMessagesModule config: " + e.getMessage());
             e.printStackTrace();
             enabled = false;
         }
     }
 
     /**
      * Handles player entering a bed and sends custom messages and sounds.
      *
      * @param event the bed enter event
      */
     @EventHandler(priority = EventPriority.HIGHEST)
     public void onPlayerBedEnter(PlayerBedEnterEvent event) {
         if (!enabled) return;
 
         Player player = event.getPlayer();
         PlayerBedEnterEvent.BedEnterResult result = event.getBedEnterResult();
         String messageKey = getMessageKeyForResult(result);
         
         if (messageKey == null) {
             return;
         }
 
         String locale = plugin.getLocaleManager().getPlayerLocale(player);
         String message = plugin.getLocaleManager().getMessage(messageKey, locale);
 
         if (message == null || message.isEmpty()) return;
 
         // Create formatted component using FormatUtil
         Component formattedMessage = FormatUtil.format(player, message);
 
         if (result != PlayerBedEnterEvent.BedEnterResult.OK) {
             event.setCancelled(true);
             // Send action bar message
             player.sendActionBar(formattedMessage);
             // Also send regular chat message
             player.sendMessage(formattedMessage);
         } else {
             // Delay message for successful bed entry
             Bukkit.getScheduler().runTaskLater(plugin, () -> {
                 if (player.isSleeping()) {
                     player.sendMessage(formattedMessage);
                 }
             }, 5L);
         }
 
         if (playSound) {
             playEnterSound(player, result == PlayerBedEnterEvent.BedEnterResult.OK);
         }
     }
     
     /**
      * Gets the message key for the bed enter result.
      *
      * @param result the bed enter result
      * @return the message key or null if no message should be sent
      */
     private String getMessageKeyForResult(PlayerBedEnterEvent.BedEnterResult result) {
         switch (result) {
             case OK:
                 return "modules.bed_messages.messages.bed_enter_success";
             case NOT_POSSIBLE_NOW:
                 return "modules.bed_messages.messages.bed_enter_not_possible_now";
             case NOT_POSSIBLE_HERE:
                 return "modules.bed_messages.messages.bed_enter_not_possible_here";
             case TOO_FAR_AWAY:
                 return "modules.bed_messages.messages.bed_enter_too_far_away";
             case NOT_SAFE:
                 return "modules.bed_messages.messages.bed_enter_not_safe";
             case OTHER_PROBLEM:
                 return "modules.bed_messages.messages.bed_enter_other_problem";
             default:
                 return null;
         }
     }
 
     /**
      * Plays the appropriate sound when entering a bed.
      *
      * @param player the player
      * @param success true if enter was successful, false otherwise
      */
     private void playEnterSound(Player player, boolean success) {
         try {
             String soundName = success ? successSoundName : errorSoundName;
             float volume = success ? successSoundVolume : errorSoundVolume;
             float pitch = success ? successSoundPitch : errorSoundPitch;
             
             Sound sound = Sound.valueOf(soundName);
             player.playSound(player.getLocation(), sound, volume, pitch);
         } catch (IllegalArgumentException e) {
             log("Invalid sound name: " + (success ? successSoundName : errorSoundName));
         } catch (Exception e) {
             log("Error playing sound: " + e.getMessage());
         }
     }
 
     /**
      * Handles player leaving a bed and sends custom messages and sounds.
      *
      * @param event the bed leave event
      */
     @EventHandler(priority = EventPriority.HIGHEST)
     public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
         if (!enabled) return;
 
         Player player = event.getPlayer();
         String locale = plugin.getLocaleManager().getPlayerLocale(player);
         String message = plugin.getLocaleManager().getMessage("modules.bed_messages.messages.bed_leave", locale);
 
         if (message == null || message.isEmpty()) return;
 
         // Create formatted component using FormatUtil
         Component formattedMessage = FormatUtil.format(player, message);
         
         // Send action bar message
         player.sendActionBar(formattedMessage);
         // Also send regular chat message
         player.sendMessage(formattedMessage);
 
         if (playSound) {
             playLeaveSound(player);
         }
     }
     
     /**
      * Plays the sound when leaving a bed.
      *
      * @param player the player
      */
     private void playLeaveSound(Player player) {
         try {
             Sound sound = Sound.valueOf(leaveSoundName);
             player.playSound(player.getLocation(), sound, leaveSoundVolume, leaveSoundPitch);
         } catch (IllegalArgumentException e) {
             log("Invalid sound name: " + leaveSoundName);
         } catch (Exception e) {
             log("Error playing sound: " + e.getMessage());
         }
     }
 }