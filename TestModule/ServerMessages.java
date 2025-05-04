package me.nagibatirowanie.originChat.Modules;

import me.nagibatirowanie.originChat.OriginChat;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.nagibatirowanie.originLib.Messages.applyPlaceholders;

public class ServerMessages extends Module implements Listener {

    private boolean joinMessageEnabled;
    private boolean leaveMessageEnabled;
    private boolean personalWelcomeEnabled;
    private List<String> joinMessages;
    private List<String> leaveMessages;
    private List<String> personalWelcomeMessages;

    public ServerMessages(OriginChat plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        if (!isEnabled()) {
            plugin.getLogger().info("The ServerMessages module is disabled in the configuration. Skip activation.");
            return;
        }

        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("The ServerMessages module has been successfully loaded.");
    }

    @Override
    public void onDisable() {
        PlayerJoinEvent.getHandlerList().unregister(this);
        PlayerQuitEvent.getHandlerList().unregister(this);
        plugin.getLogger().info("The ServerMessages module is disabled.");
    }

    protected void loadConfig() {
        try {
        joinMessageEnabled = plugin.getConfig().getBoolean("server_messages.join_message_enabled", true);
        leaveMessageEnabled = plugin.getConfig().getBoolean("server_messages.leave_message_enabled", true);
        personalWelcomeEnabled = plugin.getConfig().getBoolean("server_messages.personal_welcome_enabled", true);
        joinMessages = plugin.getConfig().getStringList("server_messages.join_messages");
        leaveMessages = plugin.getConfig().getStringList("server_messages.leave_messages");
        personalWelcomeMessages = plugin.getConfig().getStringList("server_messages.personal_welcome_messages");
        } catch (Exception e) {
            plugin.getLogger().severe("❗Failed to load ServerMessages configuration: " + e.getMessage());
    }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (personalWelcomeEnabled && !personalWelcomeMessages.isEmpty()) {
            String welcomeMessage = getRandomMessage(personalWelcomeMessages);
            player.sendMessage(applyPlaceholders(player, welcomeMessage));
        }

        if (joinMessageEnabled && !joinMessages.isEmpty()) {
            String joinMessage = getRandomMessage(joinMessages);
            event.setJoinMessage(applyPlaceholders(player, joinMessage));
        } else {
            event.setJoinMessage(null);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (leaveMessageEnabled && !leaveMessages.isEmpty()) {
            String leaveMessage = getRandomMessage(leaveMessages);
            event.setQuitMessage(applyPlaceholders(player, leaveMessage));
        } else {
            event.setQuitMessage(null);
        }
    }

    private String getRandomMessage(List<String> messages) {
        return messages.get(new Random().nextInt(messages.size()));
    }


}