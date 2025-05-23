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
 import net.kyori.adventure.title.Title;
 import org.bukkit.Bukkit;
 import org.bukkit.configuration.ConfigurationSection;
 import org.bukkit.entity.Player;
 import org.bukkit.scheduler.BukkitTask;
 
 import java.time.Duration;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Random;
 
 /**
  * Модуль для отправки автоматических сообщений игрокам через заданные интервалы времени
  * с поддержкой различных типов сообщений (чат, actionbar, title, subtitle) и локализации.
  */
 public class AutoMessagesModule extends AbstractModule {
 
     private final Map<String, AutoMessage> messages = new HashMap<>();
     private final Map<String, BukkitTask> tasks = new HashMap<>();
     private final Random random = new Random();
     private boolean enabled;
 
     /**
      * Конструктор модуля автосообщений
      * @param plugin экземпляр основного плагина
      */
     public AutoMessagesModule(OriginChat plugin) {
         super(plugin, "auto_messages", "Auto Messages", 
               "Sends automatic messages to players at configured intervals", "1.0");
     }
 
     @Override
     public void onEnable() {
         loadModuleConfig("modules/auto_messages");
         if (config == null) {
             log("Failed to load Auto Messages configuration.");
             return;
         }
 
         enabled = config.getBoolean("enabled", true);
         if (!enabled) {
             log("Auto Messages module is disabled in configuration.");
             return;
         }
 
         loadMessages();
         startTasks();
         log("Auto Messages module enabled successfully.");
     }
 
     @Override
     public void onDisable() {
         stopTasks();
         messages.clear();
         log("Auto Messages module disabled.");
     }
 
     /**
      * Загружает сообщения из конфигурации
      */
     private void loadMessages() {
         messages.clear();
         ConfigurationSection messagesSection = config.getConfigurationSection("messages");
         if (messagesSection == null) {
             log("No messages configured in auto_messages.yml");
             return;
         }
 
         for (String messageId : messagesSection.getKeys(false)) {
             ConfigurationSection messageSection = messagesSection.getConfigurationSection(messageId);
             if (messageSection == null) continue;
 
             try {
                 String type = messageSection.getString("type", "chat").toLowerCase();
                 int interval = messageSection.getInt("interval", 300); // интервал в секундах, по умолчанию 5 минут
                 boolean random = messageSection.getBoolean("random", false);
 
                 AutoMessage message = new AutoMessage(messageId, type, interval, random);
 
                 // Проверяем, есть ли локализованные сообщения
                 if (messageSection.contains("message")) {
                     if (messageSection.isString("message")) {
                         // Одиночное сообщение без локализации
                         message.addMessage("default", messageSection.getString("message"));
                     } else if (messageSection.isList("message")) {
                         // Список сообщений без локализации
                         List<String> messageList = messageSection.getStringList("message");
                         for (String msg : messageList) {
                             message.addMessage("default", msg);
                         }
                     } else if (messageSection.isConfigurationSection("message")) {
                         // Локализованные сообщения
                         ConfigurationSection localeSection = messageSection.getConfigurationSection("message");
                         for (String localeOrTipId : localeSection.getKeys(false)) {
                             ConfigurationSection tipSection = localeSection.getConfigurationSection(localeOrTipId);
                             
                             if (tipSection != null) {
                                 // Это вложенная секция с идентификаторами советов (tip1, tip2, ...)
                                 for (String locale : tipSection.getKeys(false)) {
                                     if (tipSection.isList(locale)) {
                                         List<String> localeMessages = tipSection.getStringList(locale);
                                         for (String msg : localeMessages) {
                                             message.addSectionMessage(localeOrTipId, locale, msg);
                                         }
                                     } else if (tipSection.isString(locale)) {
                                         message.addSectionMessage(localeOrTipId, locale, tipSection.getString(locale));
                                     }
                                 }
                             } else {
                                 // Обычная локализация
                                 if (localeSection.isString(localeOrTipId)) {
                                     message.addMessage(localeOrTipId, localeSection.getString(localeOrTipId));
                                 } else if (localeSection.isList(localeOrTipId)) {
                                     List<String> localeMessages = localeSection.getStringList(localeOrTipId);
                                     for (String msg : localeMessages) {
                                         message.addMessage(localeOrTipId, msg);
                                     }
                                 }
                             }
                         }
                     }
                 }
 
                 // Добавляем настройки title/subtitle если тип title или subtitle
                 if (type.equals("title") || type.equals("subtitle")) {
                     int fadeIn = messageSection.getInt("fade_in", 10);
                     int stay = messageSection.getInt("stay", 70);
                     int fadeOut = messageSection.getInt("fade_out", 20);
                     message.setTitleSettings(fadeIn, stay, fadeOut);
 
                     // Если это subtitle, нужен title
                     if (type.equals("subtitle")) {
                         String title = messageSection.getString("title", "");
                         message.setTitle(title);
                     }
                 }
 
                 messages.put(messageId, message);
                 log("Loaded auto message: " + messageId + " (type: " + type + ", interval: " + interval + "s)");
             } catch (Exception e) {
                 log("Error loading message " + messageId + ": " + e.getMessage());
                 e.printStackTrace();
             }
         }
 
         log("Loaded " + messages.size() + " auto messages.");
     }
 
     /**
      * Запускает задачи для отправки сообщений
      */
     private void startTasks() {
         stopTasks(); // Останавливаем существующие задачи перед запуском новых
 
         for (Map.Entry<String, AutoMessage> entry : messages.entrySet()) {
             String messageId = entry.getKey();
             AutoMessage message = entry.getValue();
 
             BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, 
                 () -> sendMessage(message), 
                 20 * message.getInterval(), // начальная задержка (в тиках)
                 20 * message.getInterval()  // интервал повторения (в тиках)
             );
 
             tasks.put(messageId, task);
             debug("Started task for message: " + messageId + " with interval " + message.getInterval() + "s");
         }
     }
 
     /**
      * Останавливает все задачи отправки сообщений
      */
     private void stopTasks() {
         for (BukkitTask task : tasks.values()) {
             task.cancel();
         }
         tasks.clear();
     }
 
     /**
      * Отправляет сообщение всем игрокам
      * @param message сообщение для отправки
      */
     private void sendMessage(AutoMessage message) {
         for (Player player : Bukkit.getOnlinePlayers()) {
             String locale = plugin.getLocaleManager().getPlayerLocale(player);
             String messageText = message.getMessage(locale, player);
             
             if (messageText == null || messageText.isEmpty()) {
                 continue;
             }
 
             // Обработка кастомных плейсхолдеров типа {player}
             String processedText = messageText.replace("{player}", player.getName());
 
             switch (message.getType()) {
                 case "chat":
                     Component chatComponent = FormatUtil.toComponent(player, processedText);
                     player.sendMessage(chatComponent);
                     break;
                 case "actionbar":
                     Component actionbarComponent = FormatUtil.toComponent(player, processedText);
                     player.sendActionBar(actionbarComponent);
                     break;
                 case "title":
                     Component titleComponent = FormatUtil.toComponent(player, processedText);
                     Title title = Title.title(
                         titleComponent,
                         Component.empty(),
                         Title.Times.times(
                             Duration.ofMillis(message.getFadeIn() * 50), // конвертируем тики в миллисекунды
                             Duration.ofMillis(message.getStay() * 50),
                             Duration.ofMillis(message.getFadeOut() * 50)
                         )
                     );
                     player.showTitle(title);
                     break;
                 case "subtitle":
                     Component titleTextComponent = FormatUtil.toComponent(player, message.getTitle().replace("{player}", player.getName()));
                     Component subtitleComponent = FormatUtil.toComponent(player, processedText);
                     Title subtitleTitle = Title.title(
                         titleTextComponent,
                         subtitleComponent,
                         Title.Times.times(
                             Duration.ofMillis(message.getFadeIn() * 50), // конвертируем тики в миллисекунды
                             Duration.ofMillis(message.getStay() * 50),
                             Duration.ofMillis(message.getFadeOut() * 50)
                         )
                     );
                     player.showTitle(subtitleTitle);
                     break;
                 default:
                     Component defaultComponent = FormatUtil.toComponent(player, processedText);
                     player.sendMessage(defaultComponent);
                     break;
             }
         }
     }
 
     /**
      * Класс для хранения информации об автоматическом сообщении
      */
     private class AutoMessage {
         private final String id;
         private final String type;
         private final int interval;
         private final boolean randomOrder;
         private final Map<String, List<String>> localizedMessages = new HashMap<>();
         private final Map<String, Integer> messageIndexes = new HashMap<>();
         // Карта для хранения сообщений по секциям (tip1, tip2, ...) для каждой локали
         private final Map<String, Map<String, List<String>>> sectionMessages = new HashMap<>();
         private final List<String> sectionIds = new ArrayList<>(); // Список идентификаторов секций
         private boolean hasSections = false; // Флаг наличия секций
         private int fadeIn = 10;
         private int stay = 70;
         private int fadeOut = 20;
         private String title = "";
 
         /**
          * Конструктор автосообщения
          * @param id идентификатор сообщения
          * @param type тип сообщения (chat, actionbar, title, subtitle)
          * @param interval интервал отправки в секундах
          * @param randomOrder случайный порядок сообщений
          */
         public AutoMessage(String id, String type, int interval, boolean randomOrder) {
             this.id = id;
             this.type = type;
             this.interval = interval;
             this.randomOrder = randomOrder;
         }
 
         /**
          * Добавляет сообщение для указанной локали
          * @param locale локаль (или "default" для сообщения без локализации)
          * @param message текст сообщения
          */
         public void addMessage(String locale, String message) {
             localizedMessages.computeIfAbsent(locale, k -> new ArrayList<>()).add(message);
         }
         
         /**
          * Добавляет сообщение для указанной секции и локали
          * @param sectionId идентификатор секции (tip1, tip2, ...)
          * @param locale локаль
          * @param message текст сообщения
          */
         public void addSectionMessage(String sectionId, String locale, String message) {
             if (!sectionIds.contains(sectionId)) {
                 sectionIds.add(sectionId);
             }
             
             Map<String, List<String>> localeMessages = sectionMessages.computeIfAbsent(locale, k -> new HashMap<>());
             localeMessages.computeIfAbsent(sectionId, k -> new ArrayList<>()).add(message);
             hasSections = true;
         }
 
         /**
          * Устанавливает настройки для title/subtitle
          * @param fadeIn время появления в тиках
          * @param stay время отображения в тиках
          * @param fadeOut время исчезновения в тиках
          */
         public void setTitleSettings(int fadeIn, int stay, int fadeOut) {
             this.fadeIn = fadeIn;
             this.stay = stay;
             this.fadeOut = fadeOut;
         }
 
         /**
          * Устанавливает заголовок для subtitle
          * @param title заголовок
          */
         public void setTitle(String title) {
             this.title = title;
         }
 
         /**
          * Получает сообщение для указанной локали
          * @param locale локаль игрока
          * @param player игрок для обработки плейсхолдеров
          * @return форматированное сообщение
          */
         public String getMessage(String locale, Player player) {
             // Если у нас есть секции сообщений (tip1, tip2, ...), обрабатываем их особым образом
             if (hasSections && randomOrder) {
                 return getRandomSectionMessage(locale, player);
             }
             
             // Стандартная обработка для обычных сообщений
             // Проверяем, есть ли сообщения для указанной локали
             List<String> messages = localizedMessages.get(locale);
             
             // Если нет, пробуем получить базовую локаль (например, "en" вместо "en_US")
             if (messages == null && locale.contains("_")) {
                 String baseLocale = locale.split("_")[0].toLowerCase();
                 messages = localizedMessages.get(baseLocale);
             }
             
             // Если все еще нет, используем дефолтную локаль из конфига
             if (messages == null) {
                 String defaultLocale = plugin.getConfigManager().getMainConfig().getString("locale.default", "en");
                 messages = localizedMessages.get(defaultLocale);
             }
             
             // Если и это не помогло, используем сообщения без локализации
             if (messages == null) {
                 messages = localizedMessages.get("default");
             }
             
             // Если сообщений нет вообще, возвращаем пустую строку
             if (messages == null || messages.isEmpty()) {
                 return "";
             }
             
             // Выбираем сообщение (случайное или по порядку)
             String message;
             if (randomOrder) {
                 message = messages.get(random.nextInt(messages.size()));
             } else {
                 // Для неслучайного порядка реализуем циклический перебор
                 int index = messageIndexes.getOrDefault(locale, 0);
                 message = messages.get(index);
                 
                 // Увеличиваем индекс для следующего вызова
                 index = (index + 1) % messages.size();
                 messageIndexes.put(locale, index);
             }
             
             return message;
         }
         
         /**
          * Получает случайное сообщение из секций для указанной локали
          * @param locale локаль игрока
          * @param player игрок для обработки плейсхолдеров
          * @return форматированное сообщение
          */
         private String getRandomSectionMessage(String locale, Player player) {
             // Проверяем, есть ли сообщения для указанной локали
             Map<String, List<String>> localeMessages = sectionMessages.get(locale);
             
             // Если нет, пробуем получить базовую локаль (например, "en" вместо "en_US")
             if ((localeMessages == null || localeMessages.isEmpty()) && locale.contains("_")) {
                 String baseLocale = locale.split("_")[0].toLowerCase();
                 localeMessages = sectionMessages.get(baseLocale);
             }
             
             // Если все еще нет, используем дефолтную локаль из конфига
             if (localeMessages == null || localeMessages.isEmpty()) {
                 String defaultLocale = plugin.getConfigManager().getMainConfig().getString("locale.default", "en");
                 localeMessages = sectionMessages.get(defaultLocale);
             }
             
             // Если и это не помогло, возвращаемся к стандартному методу
             if (localeMessages == null || localeMessages.isEmpty()) {
                 return getMessage(locale, player);
             }
             
             // Выбираем случайную секцию
             String randomSection = sectionIds.get(random.nextInt(sectionIds.size()));
             
             // Проверяем, есть ли сообщения в этой секции для данной локали
             List<String> messages = localeMessages.get(randomSection);
             if (messages == null || messages.isEmpty()) {
                 // Если в выбранной секции нет сообщений для этой локали, выбираем любую доступную секцию
                 for (String sectionId : sectionIds) {
                     messages = localeMessages.get(sectionId);
                     if (messages != null && !messages.isEmpty()) {
                         randomSection = sectionId;
                         break;
                     }
                 }
             }
             
             // Если все еще нет сообщений, возвращаемся к стандартному методу
             if (messages == null || messages.isEmpty()) {
                 return getMessage(locale, player);
             }
             
             // Выбираем случайное сообщение из выбранной секции
             String message = messages.get(random.nextInt(messages.size()));
             
             return message;
         }
 
         public String getId() {
             return id;
         }
 
         public String getType() {
             return type;
         }
 
         public int getInterval() {
             return interval;
         }
 
         public int getFadeIn() {
             return fadeIn;
         }
 
         public int getStay() {
             return stay;
         }
 
         public int getFadeOut() {
             return fadeOut;
         }
 
         public String getTitle() {
             return title;
         }
     }
 }