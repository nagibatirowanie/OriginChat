package me.nagibatirowanie.originChat.Modules;

import me.nagibatirowanie.originChat.OriginChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;



import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static me.nagibatirowanie.originLib.Messages.replaceHexColors;
import static me.nagibatirowanie.originLib.Messages.applyPlaceholders;

public class Chat extends Module implements Listener {

    private final Map<String, ChatConfig> chatConfigs = new HashMap<>();
    private boolean registered = false;
    public Chat(OriginChat plugin) {
        super(plugin);
        moderator = moder;
    }

    @Override
    public void onEnable() {
        if (!isEnabled()) {
            plugin.getLogger().info("The chat module is disabled in the configuration. Skip activation.");
            return;
        }

        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        loadChatConfigs();
        plugin.getLogger().info("The chat module has been successfully loaded.");
    }

    @Override
    public void onDisable() {
        if (!registered) {
            plugin.getLogger().info("");
            return;
        }

        HandlerList.unregisterAll(this);
        registered = false;
        plugin.getLogger().info("The chat module is disabled.");
    }

    private void loadChatConfigs() {
        try {
            chatConfigs.clear();
            ConfigurationSection chatsSection = plugin.getConfig().getConfigurationSection("chats");
            if (chatsSection != null) {
                for (String chatName : chatsSection.getKeys(false)) {
                    ConfigurationSection chatSection = chatsSection.getConfigurationSection(chatName);
                    if (chatSection != null) {
                        ChatConfig config = new ChatConfig(
                                chatSection.getString("prefix", ""),
                                chatSection.getInt("radius", -1),
                                chatSection.getString("format", "#f0f0f0[{chat}] {player}: {message}"),
                                chatSection.getString("permission-write", ""),
                                chatSection.getString("permission-view", "")
                        );
                        chatConfigs.put(chatName, config);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("❗Failed to load Chat configuration: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!isEnabled()) {
            return;
        }


        Player player = event.getPlayer();
        String message = event.getMessage();
        event.setCancelled(true);

        for (ChatConfig config : chatConfigs.values()) {
            if (message.startsWith(config.getPrefix())) {
                if (!config.getPrefix().isEmpty()) {
                    message = message.substring(config.getPrefix().length());
                }

                if (!config.getPermissionWrite().isEmpty() && !player.hasPermission(config.getPermissionWrite())) {
                    String noPermissionMessage = getMessageFromConfig("no-permission");
                    player.sendMessage(noPermissionMessage);
                    return;
                }

                String formattedMessage = applyPlaceholders(player, config.getFormat())
                        .replace("{message}", message);

                if (config.getRadius() > 0) {
                    boolean heard = false;
                    for (Player target : player.getWorld().getPlayers()) {
                        if (target.equals(player)) continue;

                        if (target.getLocation().distance(player.getLocation()) <= config.getRadius() &&
                                (config.getPermissionView().isEmpty() || target.hasPermission(config.getPermissionView()))) {
                            target.sendMessage(formattedMessage);
                            heard = true;
                        }
                    }

                    if (!heard) {
                        String nobodyHeard = getMessageFromConfig("nobody-heard");
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', replaceHexColors(nobodyHeard)));
                    }

                    player.sendMessage(formattedMessage);
                } else {
                    Bukkit.getOnlinePlayers().stream()
                            .filter(p -> config.getPermissionView().isEmpty() || p.hasPermission(config.getPermissionView()))
                            .forEach(p -> p.sendMessage(formattedMessage));
                }

                return;
            }
        }

        player.sendMessage(getMessageFromConfig("chat-not-found"));
    }


    private String getMessageFromConfig(String path) {
        String message = plugin.getMessagesConfig().getString(path, "❗null");
        return ChatColor.translateAlternateColorCodes('&', replaceHexColors(message));
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