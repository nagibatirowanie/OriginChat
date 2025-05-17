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
 import me.nagibatirowanie.originchat.utils.ColorUtil;
 import org.bukkit.Bukkit;
 import org.bukkit.entity.Player;
 import org.bukkit.event.EventHandler;
 import org.bukkit.event.EventPriority;
 import org.bukkit.event.Listener;
 import org.bukkit.event.player.PlayerBedEnterEvent;
 import org.bukkit.event.player.PlayerBedLeaveEvent;
 import net.md_5.bungee.api.ChatMessageType;
 import net.md_5.bungee.api.chat.TextComponent;
 
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
         Bukkit.getPluginManager().registerEvents(this, plugin);
         log("Bed messages module loaded.");
     }
 
     @Override
     public void onDisable() {
         PlayerBedEnterEvent.getHandlerList().unregister(this);
         PlayerBedLeaveEvent.getHandlerList().unregister(this);
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
         } catch (Exception e) {
             plugin.getPluginLogger().severe("Error loading BedMessagesModule config: " + e.getMessage());
             e.printStackTrace();
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
         String messageKey;
 
         switch (result) {
             case OK:
                 messageKey = "modules.bed_messages.messages.bed_enter_success";
                 break;
             case NOT_POSSIBLE_NOW:
                 messageKey = "modules.bed_messages.messages.bed_enter_not_possible_now";
                 break;
             case NOT_POSSIBLE_HERE:
                 messageKey = "modules.bed_messages.messages.bed_enter_not_possible_here";
                 break;
             case TOO_FAR_AWAY:
                 messageKey = "modules.bed_messages.messages.bed_enter_too_far_away";
                 break;
             case NOT_SAFE:
                 messageKey = "modules.bed_messages.messages.bed_enter_not_safe";
                 break;
             case OTHER_PROBLEM:
                 messageKey = "modules.bed_messages.messages.bed_enter_other_problem";
                 break;
             default:
                 return;
         }
 
         String locale = plugin.getLocaleManager().getPlayerLocale(player);
         String message = plugin.getLocaleManager().getMessage(messageKey, locale);
 
         if (message == null || message.isEmpty()) return;
 
         if (result != PlayerBedEnterEvent.BedEnterResult.OK) {
             event.setCancelled(true);
             player.spigot().sendMessage(
                 ChatMessageType.ACTION_BAR,
                 TextComponent.fromLegacyText(ColorUtil.format(player, message))
             );
             player.sendMessage(ColorUtil.format(player, message));
         } else {
             Bukkit.getScheduler().runTaskLater(plugin, () -> {
                 if (player.isSleeping()) {
                     player.sendMessage(ColorUtil.format(player, message));
                 }
             }, 5L);
         }
 
         if (playSound) {
             playEnterSound(player, result == PlayerBedEnterEvent.BedEnterResult.OK);
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
             player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(soundName), volume, pitch);
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
 
         player.spigot().sendMessage(
             ChatMessageType.ACTION_BAR,
             TextComponent.fromLegacyText(ColorUtil.format(player, message))
         );
         player.sendMessage(ColorUtil.format(player, message));
 
         if (playSound) {
             try {
                 player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(leaveSoundName), leaveSoundVolume, leaveSoundPitch);
             } catch (Exception e) {
                 log("Error playing sound: " + e.getMessage());
             }
         }
     }
 }
 