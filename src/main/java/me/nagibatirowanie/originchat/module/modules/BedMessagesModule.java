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
  * Module for customizing bed interaction messages
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
 
     // Load settings from config
     protected void loadConfig() {
         try {
             enabled = config.getBoolean("enabled", true);
             playSound = config.getBoolean("play_sound", true);
             
             // Загружаем настройки звуков
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
             plugin.getPluginLogger().severe("❗ Error loading BedMessagesModule config: " + e.getMessage());
             e.printStackTrace();
         }
     }
 
     /**
      * Handle player entering a bed
      */
     @EventHandler(priority = EventPriority.HIGHEST)
     public void onPlayerBedEnter(PlayerBedEnterEvent event) {
         if (!enabled) return;
         
         Player player = event.getPlayer();
         String messageKey;
         
         // Определяем ключ сообщения в зависимости от результата события
         switch (event.getBedEnterResult()) {
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
                 return; // Неизвестный результат, не обрабатываем
         }
         
         // Получаем локализованное сообщение из файла локализации
         String locale = plugin.getLocaleManager().getPlayerLocale(player);
         String message = plugin.getLocaleManager().getMessage(messageKey, locale);
         
         if (message != null && !message.isEmpty()) {
             // Для неудачных попыток отменяем событие и отправляем кастомное сообщение
             if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
                 // Отменяем стандартное сообщение
                 event.setCancelled(true);
                 
                 // Отправляем сообщение в action bar (над инвентарем)
                 player.spigot().sendMessage(
                     net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                     net.md_5.bungee.api.chat.TextComponent.fromLegacyText(ColorUtil.format(player, message))
                 );
                 
                 // Также отправляем сообщение в чат для большей наглядности
                 player.sendMessage(ColorUtil.format(player, message));
             } else {
                 // Для успешного входа не отменяем событие, но отправляем кастомное сообщение
                 Bukkit.getScheduler().runTaskLater(plugin, () -> {
                     if (player.isSleeping()) {
                         player.sendMessage(ColorUtil.format(player, message));
                     }
                 }, 5L); // 5 тиков (~0.25 секунды)
             }
             
             // Воспроизводим звук, если эта опция включена
             if (playSound) {
                 try {
                     // Выбираем звук в зависимости от результата
                     String soundName;
                     float volume;
                     float pitch;
                     
                     if (event.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) {
                         // Звук успешного входа в кровать
                         soundName = successSoundName;
                         volume = successSoundVolume;
                         pitch = successSoundPitch;
                     } else {
                         // Звук ошибки
                         soundName = errorSoundName;
                         volume = errorSoundVolume;
                         pitch = errorSoundPitch;
                     }
                     
                     player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(soundName), volume, pitch);
                 } catch (Exception e) {
                     log("Error playing sound: " + e.getMessage());
                 }
             }
         }
     }
 
     /**
      * Handle player leaving a bed
      */
     @EventHandler(priority = EventPriority.HIGHEST)
     public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
         if (!enabled) return;
         
         Player player = event.getPlayer();
         String messageKey = "modules.bed_messages.messages.bed_leave";
         
         // Получаем локализованное сообщение из файла локализации
         String locale = plugin.getLocaleManager().getPlayerLocale(player);
         String message = plugin.getLocaleManager().getMessage(messageKey, locale);
         
         if (message != null && !message.isEmpty()) {
             // Отправляем сообщение в action bar
             player.spigot().sendMessage(
                 ChatMessageType.ACTION_BAR,
                 TextComponent.fromLegacyText(ColorUtil.format(player, message))
             );
             
             // И также в обычный чат
             player.sendMessage(ColorUtil.format(player, message));
             
             // Воспроизводим звук при выходе из кровати, если эта опция включена
             if (playSound) {
                 try {
                     // Используем звук для выхода из кровати
                     player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(leaveSoundName), leaveSoundVolume, leaveSoundPitch);
                 } catch (Exception e) {
                     log("Error playing sound: " + e.getMessage());
                 }
             }
         }
     }
 }