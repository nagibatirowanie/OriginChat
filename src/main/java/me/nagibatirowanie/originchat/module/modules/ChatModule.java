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
      * Returns the map of chat configurations
      * @return map of chat configurations
      */
     public Map<String, ChatConfig> getChatConfigs() {
         return chatConfigs;
     }
 
     private LocaleManager localeManager;
     private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
     // Cache for storing player cooldowns
     private final Map<UUID, Integer> playerCooldownCache = new ConcurrentHashMap<>();
     // Last cache update time for each player
     private final Map<UUID, Long> cooldownCacheUpdateTime = new ConcurrentHashMap<>();
     private int defaultCooldown = 3;
     private boolean cooldownEnabled = true;
     // Cache TTL in milliseconds (1 minute)
     private static final long CACHE_TTL = 60000;
     
     private boolean translationEnabled = true;
     
     // Reference to the chat bubbles module for integration
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
         
         // Register event handlers and commands
         plugin.getServer().getPluginManager().registerEvents(this, plugin);
         plugin.getCommand("translatetoggle").setExecutor(this);
 
         localeManager = plugin.getLocaleManager();
         
         // Get reference to chat bubbles module for integration
         try {
             chatBubblesModule = (ChatBubblesModule) plugin.getModuleManager().getModule("chat_bubbles");
             if (chatBubblesModule != null) {
                 log("Integration with Chat Bubbles module successfully established");
             } else {
                 log("Chat Bubbles module not found, integration not possible");
             }
         } catch (Exception e) {
             log("❗ Error when obtaining Chat Bubbles module: " + e.getMessage());
             e.printStackTrace();
         }
         
         // Start a task to clean expired cooldown cache every minute
         Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::clearExpiredCooldownCache, 1200L, 1200L);
         
         // Initialize last message time for all online players
         for (Player player : Bukkit.getOnlinePlayers()) {
             lastMessageTime.put(player.getUniqueId(), 0L);
             plugin.getPluginLogger().info("[ChatModule] Last message time initialized for player " + player.getName());
         }
     }
 
     @Override
     public void onDisable() {
         HandlerList.unregisterAll(this);
         // Clear the last message time cache
         lastMessageTime.clear();
         plugin.getPluginLogger().info("[ChatModule] Last message time cache cleared");
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
             
             // Clear cooldown cache when reloading config
             playerCooldownCache.clear();
             cooldownCacheUpdateTime.clear();
             loadChatConfigs();
         } catch (Exception e) {
             log("❗ Error when loading chat configuration: " + e.getMessage());
             e.printStackTrace();
         }
     }
 
     /**
      * Loads chat configurations from config file
      */
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
                     debug("Chat loaded: " + chatName + ", prefix: " + chatConfig.getPrefix());
                 }
             }
         }
 
         if (chatConfigs.isEmpty()) {
             ChatConfig defaultChat = new ChatConfig(
                     "", -1, "<gray>[{player}]</gray> <white>{message}</white>", "", ""
             );
             chatConfigs.put("global", defaultChat);
             debug("Default chat added as no configured chats were found.");
         }
     }
 
     /**
      * Gets the cooldown value for a player based on their permissions
      * Looks for permissions in the format originchat.chat.cooldown.<number> and returns the lowest value
      * Uses caching for performance optimization
      * 
      * @param player the player
      * @return cooldown value in seconds
      */
     private int getPlayerCooldown(Player player) {
         UUID playerUUID = player.getUniqueId();
         
         // Check if there's a cached value and if it's not expired
         if (playerCooldownCache.containsKey(playerUUID)) {
             long lastUpdateTime = cooldownCacheUpdateTime.getOrDefault(playerUUID, 0L);
             if (System.currentTimeMillis() - lastUpdateTime < CACHE_TTL) {
                 int cachedCooldown = playerCooldownCache.get(playerUUID);
                 return cachedCooldown;
             }
         }
         
         // If player has admin permission, return 0 (no cooldown)
         if (player.hasPermission("originchat.admin")) {
             plugin.getPluginLogger().debug("[ChatModule] Player " + player.getName() + " has admin permission, cooldown disabled");
             updateCooldownCache(playerUUID, 0);
             return 0;
         }
         
         // Look for all permissions in the format originchat.chat.cooldown.<number>
         int minCooldown = defaultCooldown;
         for (int i = 0; i <= 60; i++) { // Check values from 0 to 60 seconds
             String permission = "originchat.chat.cooldown." + i;
             if (player.hasPermission(permission) && i < minCooldown) {
                 minCooldown = i;
             }
         }
         
         // Update cache
         updateCooldownCache(playerUUID, minCooldown);
                  
         return minCooldown;
     }
     
     /**
      * Updates cooldown cache for a player
      * 
      * @param playerUUID player's UUID
      * @param cooldownValue cooldown value
      */
     private void updateCooldownCache(UUID playerUUID, int cooldownValue) {
         playerCooldownCache.put(playerUUID, cooldownValue);
         cooldownCacheUpdateTime.put(playerUUID, System.currentTimeMillis());
     }
     
     /**
      * Clears expired entries from the cooldown cache
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
      * Handler for translatetoggle command
      */
     @Override
     public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
         if (!(sender instanceof Player)) {
             sender.sendMessage("§cThis command is only available for players");
             return true;
         }
         
         Player player = (Player) sender;
         
         if (!translationEnabled) {
             player.sendMessage(formatMessage("§cAuto-translation feature is disabled on this server."));
             return true;
         }
         
         boolean newState = plugin.getTranslateManager().toggleTranslate(player);
         
         if (newState) {
             localeManager.sendMessage(player, "commands.translate_enabled");
         } else {
             localeManager.sendMessage(player, "commands.translate_disabled");
         }
         
         return true;
     }
     
     /**
      * Checks if auto-translation is enabled for a player
      * 
      * @param player the player
      * @return true if auto-translation is enabled
      */
     public boolean isTranslateEnabled(Player player) {
         return plugin.getTranslateManager().isTranslateEnabled(player);
     }
     
     /**
      * Handler for player join event
      * Initializes last message time
      */
     @EventHandler
     public void onPlayerJoin(PlayerJoinEvent event) {
         if (!enabled) {
             return;
         }
         
         Player player = event.getPlayer();
         lastMessageTime.put(player.getUniqueId(), 0L);
         //plugin.getPluginLogger().info("[ChatModule] Last message time initialized for player " + player.getName());
     }
 
     /**
      * Handler for player chat event
      */
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
             
             // Check if player exists in last message time cache
             if (!lastMessageTime.containsKey(player.getUniqueId())) {
                 lastMessageTime.put(player.getUniqueId(), 0L);
             }
             
             if (cooldown > 0) {
                 long last = lastMessageTime.getOrDefault(player.getUniqueId(), 0L);
                 long diff = (now - last) / 1000;
                 //plugin.getPluginLogger().info("[ChatModule] Checking cooldown for player " + player.getName() + ": " + diff + " sec. passed out of " + cooldown + " sec.");
                 if (diff < cooldown) {
                     String msg = plugin.getConfigManager().getLocalizedMessage("chat", "messages.cooldown", player.getLocale().toString()).replace("{cooldown}", String.valueOf(cooldown - diff));
                     player.sendMessage(formatMessage(msg));
                     //plugin.getPluginLogger().info("[ChatModule] Player " + player.getName() + " message blocked due to cooldown. Time remaining: " + (cooldown - diff) + " sec.");
                     event.setCancelled(true);
                     return;
                 }
             }
             
             // Always update last message time, even if cooldown is 0
             //plugin.getPluginLogger().info("[ChatModule] Updating last message time for player " + player.getName());
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
                 
                 // Save final message for use in lambdas
                 final String finalMessage = message;
                 String formattedMessage = formatChatMessage(player, finalMessage, chatConfig, chatName);
                 
                 // Create chat bubble if module is available
                 if (chatBubblesModule != null) {
                     // Call createChatBubble method in main thread with chat name
                     final String finalChatName = chatName;
                     plugin.getServer().getScheduler().runTask(plugin, () -> {
                         chatBubblesModule.createChatBubble(player, finalMessage, finalChatName);
                         debug("Chat bubble created for player " + player.getName() + " message in chat '" + finalChatName + "'");
                     });
                 }
                 
                 // If translation is disabled on the server, send message to everyone as usual
                 if (!translationEnabled) {
                     sendMessageToPlayers(player, formattedMessage, chatConfig);
                     return;
                 }
                 
                 // Send message to players with translation disabled
                 List<Player> playersWithTranslation = new ArrayList<>();
                 
                 if (chatConfig.getRadius() > 0) {
                     // For local chat
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
                             if (!targetTranslateEnabled) {
                                 
                                 target.sendMessage(formattedMessage);
                             } else {
                                 
                                 playersWithTranslation.add(target);
                             }
                         }
                     }
                     if (!heard) {
                         player.sendMessage(formatMessage(plugin.getConfigManager().getLocalizedMessage("chat", "messages.nobody-heard", player.getLocale().toString())));
                     }
                 } else {
                     // For global chat
                     for (Player target : Bukkit.getOnlinePlayers()) {
                         if (target.equals(player)) {
                             target.sendMessage(formattedMessage);
                             continue;
                         }
                         
                         if (chatConfig.getPermissionView().isEmpty() || target.hasPermission(chatConfig.getPermissionView())) {
                             boolean targetTranslateEnabled = plugin.getTranslateManager().isTranslateEnabled(target);
                             
                             if (!targetTranslateEnabled) {
                                 
                                 target.sendMessage(formattedMessage);
                             } else {
                                 
                                 playersWithTranslation.add(target);
                             }
                         }
                     }
                 }
                 
                 // If there are no players with translation enabled, finish processing
                 if (playersWithTranslation.isEmpty()) {
                     return;
                 }
                 
                 // Collect unique player locales
                 Set<String> uniqueLocales = playersWithTranslation.stream()
                         .map(p -> plugin.getLocaleManager().getPlayerLocaleRaw(p))
                         .collect(Collectors.toSet());
                 // Map for storing translated messages
                 Map<String, String> translatedMessages = new ConcurrentHashMap<>();
                 List<CompletableFuture<Void>> translationFutures = new ArrayList<>();
                 String senderLocale = plugin.getLocaleManager().getPlayerLocaleRaw(player);
                 // Start async translation for each locale
                 for (String locale : uniqueLocales) {
                     // Skip translation only if locale fully matches sender locale
                     if (locale.equals(senderLocale)) {
                         translatedMessages.put(locale, finalMessage);
                         continue;
                     }
                     CompletableFuture<Void> future = TranslateUtil.translateAsync(finalMessage, locale)
                             .thenAccept(translatedMessage -> {
                                 // Save translated message
                                 translatedMessages.put(locale, translatedMessage);
                             })
                             .exceptionally(ex -> {
                                 plugin.getPluginLogger().warning("Error translating message to " + locale + ": " + ex.getMessage());
                                 // Use original message in case of error
                                 translatedMessages.put(locale, finalMessage);
                                 return null;
                             });
                     translationFutures.add(future);
                 }
                 
                 // Wait for all translations to complete
                 CompletableFuture.allOf(translationFutures.toArray(new CompletableFuture[0]))
                         .thenRun(() -> {
                             // Send translated messages to players
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
      * Sends message to players according to chat settings
      * 
      * @param sender message sender
      * @param formattedMessage formatted message
      * @param chatConfig chat configuration
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
 
     /**
      * Formats a chat message with placeholders and styling
      * 
      * @param player the player sending the message
      * @param message the message content
      * @param config the chat configuration
      * @param chatName the name of the chat
      * @return formatted message string
      */
     private String formatChatMessage(Player player, String message, ChatConfig config, String chatName) {
         // 1) Raw format from config with immutable placeholders
         String raw = config.getFormat()
             .replace("{player}", player.getName())
             .replace("{chat}", chatName)
             .replace("{world}", player.getWorld().getName());
     
         // 2) Marker instead of {message}
         final String MSG_MARKER = "&&MSG&&";
         String withMarker = raw.replace("{message}", MSG_MARKER);
     
         // 3) Full formatting of prefix and suffix:
         //    - allowColors = true -> all color tags from config,
         //    - useMiniMessage = miniMessage,
         //    - allowPlaceholders = true -> all %...% are expanded
         String allConfigFormatted = ColorUtil.format(
             player,
             withMarker,
             /* allowColors= */ true,
             /* useMiniMessage= */ miniMessage,
             /* allowPlaceholders= */ true
         );
     
         // 4) Split by marker to remove it from final string
         String[] parts = allConfigFormatted.split(Pattern.quote(MSG_MARKER), -1);
         String prefix = parts.length > 0 ? parts[0] : "";
         String suffix = parts.length > 1 ? parts[1] : "";
     
         // 5) Check player permissions for colors and placeholders
         boolean canColors       = player.hasPermission("originchat.format.colors");
         boolean canPlaceholders = player.hasPermission("originchat.format.placeholders");
     
         // 6) Format player's message text:
         //    - if has permission for colors -> allow HEX and MiniMessage,
         //      and expand %...% only if allowPlaceholders is true
         //    - if no permission for colors -> disable any colors/MiniMessage,
         //      but still can expand %...% if allowPlaceholders is true
         String msgFormatted = ColorUtil.format(
             player,
             message,
             /* allowColors=       */ canColors && hexColors,
             /* useMiniMessage=    */ canColors && miniMessage,
             /* allowPlaceholders= */ canPlaceholders
         );
     
         // 7) Glue everything together and return
         return prefix + msgFormatted + suffix;
     }
     
     /**
      * Format message with placeholders
      * For system messages always use full formatting
      * 
      * @param message message to format
      * @return formatted message
      */
     private String formatMessage(String message) {
         return ColorUtil.format(message, true, true, true);
     }
 
     /**
      * Chat configuration class
      */
     private static class ChatConfig {
         private final String prefix;
         private final int radius;
         private final String format;
         private final String permissionWrite;
         private final String permissionView;
 
         /**
          * Creates a new chat configuration
          * 
          * @param prefix chat prefix
          * @param radius chat radius (-1 for global)
          * @param format message format
          * @param permissionWrite permission to write in chat
          * @param permissionView permission to view messages in chat
          */
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