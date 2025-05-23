package me.nagibatirowanie.originchat.module.modules.servermessages;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.utils.FormatUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Submodule for handling gamemode change messages with localization support.
 */
public class GamemodeMessagesSubmodule implements Listener {
    private final OriginChat plugin;
    private final ServerMessagesModule parentModule;

    // Configuration flags
    private boolean gamemodeMessageEnabled;
    private boolean disableVanillaMessages;
    
    // Patterns for gamemode commands
    private static final Pattern GAMEMODE_PATTERN = Pattern.compile(
            "^/(gamemode|gm|adventure|creative|survival|spectator|minecraft:(gamemode|adventure|creative|survival|spectator))\\s*(\\d|[a-zA-Z]+)?\\s*([\\w]+)?$", 
            Pattern.CASE_INSENSITIVE);

    public GamemodeMessagesSubmodule(OriginChat plugin, ServerMessagesModule parentModule) {
        this.plugin = plugin;
        this.parentModule = parentModule;
    }

    /**
     * Initializes the submodule.
     */
    public void initialize() {
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getPluginLogger().info("[ServerMessages] GamemodeMessagesSubmodule initialized.");
    }

    /**
     * Shuts down the submodule.
     */
    public void shutdown() {
        PlayerCommandPreprocessEvent.getHandlerList().unregister(this);
        plugin.getPluginLogger().info("[ServerMessages] GamemodeMessagesSubmodule disabled.");
    }

    /**
     * Loads the submodule's configuration.
     */
    public void loadConfig() {
        gamemodeMessageEnabled = parentModule.getConfig().getBoolean("gamemode_message_enabled", true);
        disableVanillaMessages = parentModule.getConfig().getBoolean("disable_vanilla_gamemode_messages", true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!gamemodeMessageEnabled) return;

        String message = event.getMessage();
        Matcher matcher = GAMEMODE_PATTERN.matcher(message);
        
        if (matcher.matches()) {
            handleGamemodeCommand(event, matcher);
        }
    }

    private void handleGamemodeCommand(PlayerCommandPreprocessEvent event, Matcher matcher) {
        final Player sender = event.getPlayer();
        final String commandBase = matcher.group(1).toLowerCase();
        String gamemodeArg = matcher.group(3);
        final String targetArg = matcher.group(4);
        
        // Determine gamemode from command or argument
        GameMode gamemode = null;
        
        // If command itself specifies the gamemode (like /survival)
        if (commandBase.equals("adventure") || commandBase.equals("minecraft:adventure")) {
            gamemode = GameMode.ADVENTURE;
        } else if (commandBase.equals("creative") || commandBase.equals("minecraft:creative")) {
            gamemode = GameMode.CREATIVE;
        } else if (commandBase.equals("survival") || commandBase.equals("minecraft:survival")) {
            gamemode = GameMode.SURVIVAL;
        } else if (commandBase.equals("spectator") || commandBase.equals("minecraft:spectator")) {
            gamemode = GameMode.SPECTATOR;
        } else if (gamemodeArg != null) {
            // If gamemode is specified as an argument
            gamemode = parseGameMode(gamemodeArg);
        }
        
        if (gamemode == null) return; // Invalid gamemode
        
        // Determine target player
        final Player targetPlayer;
        if (targetArg != null && !targetArg.isEmpty()) {
            targetPlayer = Bukkit.getPlayerExact(targetArg);
            if (targetPlayer == null) {
                // Send localized message that player not found
                sendPlayerNotFoundMessage(sender, targetArg);
                event.setCancelled(true);
                return;
            }
        } else {
            targetPlayer = sender; // Self-target
        }
        
        final GameMode finalGamemode = gamemode;
        
        if (disableVanillaMessages) {
            // Cancel the original command
            event.setCancelled(true);
            
            // Execute the command via the server
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Remember the player's current gamemode
                final GameMode previousGamemode = targetPlayer.getGameMode();
                
                // Execute the command as console
                boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                        "gamemode " + finalGamemode.name().toLowerCase() + " " + targetPlayer.getName());
                
                if (success) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        // Check if gamemode actually changed
                        if (targetPlayer.getGameMode() != previousGamemode) {
                            // Send custom messages
                            sendGamemodeChangeMessages(sender, targetPlayer, finalGamemode);
                        } else {
                            // If gamemode didn't change, send localized message
                            sendSameGamemodeMessage(sender, targetPlayer, finalGamemode);
                        }
                    }, 2L);
                }
            });
        } else {
            // If vanilla messages are not disabled, just send our additional ones
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                sendGamemodeChangeMessages(sender, targetPlayer, finalGamemode);
            }, 2L);
        }
    }

    private GameMode parseGameMode(String arg) {
        // Try to parse numeric gamemode
        try {
            int mode = Integer.parseInt(arg);
            switch (mode) {
                case 0: return GameMode.SURVIVAL;
                case 1: return GameMode.CREATIVE;
                case 2: return GameMode.ADVENTURE;
                case 3: return GameMode.SPECTATOR;
                default: return null;
            }
        } catch (NumberFormatException e) {
            // Try to parse text gamemode
            try {
                return GameMode.valueOf(arg.toUpperCase());
            } catch (IllegalArgumentException ex) {
                // Try to match partial names
                if (arg.equalsIgnoreCase("s") || arg.equalsIgnoreCase("su") || 
                    arg.equalsIgnoreCase("sur") || arg.equalsIgnoreCase("surv")) {
                    return GameMode.SURVIVAL;
                } else if (arg.equalsIgnoreCase("c") || arg.equalsIgnoreCase("cr") || 
                           arg.equalsIgnoreCase("cre") || arg.equalsIgnoreCase("creat")) {
                    return GameMode.CREATIVE;
                } else if (arg.equalsIgnoreCase("a") || arg.equalsIgnoreCase("ad") || 
                           arg.equalsIgnoreCase("adv") || arg.equalsIgnoreCase("advent")) {
                    return GameMode.ADVENTURE;
                } else if (arg.equalsIgnoreCase("sp") || arg.equalsIgnoreCase("spec") || 
                           arg.equalsIgnoreCase("spect") || arg.equalsIgnoreCase("specta")) {
                    return GameMode.SPECTATOR;
                }
            }
        }
        return null;
    }

    /**
     * Sends a localized message when a player is not found.
     */
    private void sendPlayerNotFoundMessage(Player sender, String targetName) {
        String message = plugin.getConfigManager().getLocalizedMessage(
                "server_messages", "gamemode_messages.player_not_found", sender);
        if (message != null) {
            sender.sendMessage(FormatUtil.toComponent(sender, 
                    message.replace("{player}", targetName)));
        } else {
            // Fallback message if localization is not available
            sender.sendMessage("Player " + targetName + " not found.");
        }
    }
    
    /**
     * Sends a localized message when a player already has the specified gamemode.
     */
    private void sendSameGamemodeMessage(Player sender, Player target, GameMode gamemode) {
        String message = plugin.getConfigManager().getLocalizedMessage(
                "server_messages", "gamemode_messages.already_in_gamemode", sender);
        if (message != null) {
            sender.sendMessage(FormatUtil.toComponent(sender, 
                    message.replace("{player}", target.getName())
                           .replace("{gamemode}", getLocalizedGamemodeName(gamemode, sender))));
        } else {
            // Fallback message if localization is not available
            sender.sendMessage("Player " + target.getName() + " is already in " + 
                    gamemode.name().toLowerCase() + " mode.");
        }
    }
    
    private void sendGamemodeChangeMessages(Player sender, Player target, GameMode gamemode) {
        // 1) Message to the sender if they're not the target
        if (!sender.equals(target)) {
            String senderMsg = plugin.getConfigManager().getLocalizedMessage(
                    "server_messages", "gamemode_messages.sender", sender);
            if (senderMsg != null) {
                sender.sendMessage(FormatUtil.toComponent(sender,
                        senderMsg.replace("{target}", target.getName())
                                .replace("{gamemode}", getLocalizedGamemodeName(gamemode, sender))));
            }
        }

        // 2) Message to the target
        String targetMsg = plugin.getConfigManager().getLocalizedMessage(
                "server_messages", "gamemode_messages.target", target);
        if (targetMsg != null) {
            target.sendMessage(FormatUtil.toComponent(target,
                    targetMsg.replace("{sender}", sender.getName())
                            .replace("{gamemode}", getLocalizedGamemodeName(gamemode, target))));
        }

        // 3) Broadcast to all ops (except sender and target)
        String broadcast = plugin.getConfigManager().getLocalizedMessage(
                "server_messages", "gamemode_messages.broadcast", sender);
        if (broadcast != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.isOp() && !p.equals(sender) && !p.equals(target)) {
                    p.sendMessage(FormatUtil.toComponent(p,
                            broadcast.replace("{sender}", sender.getName())
                                    .replace("{target}", target.getName())
                                    .replace("{gamemode}", getLocalizedGamemodeName(gamemode, p))));
                }
            }
        }
    }

    /**
     * Gets the localized name of a gamemode for a player.
     */
    private String getLocalizedGamemodeName(GameMode gamemode, Player player) {
        String key = "gamemode_messages.modes." + gamemode.name().toLowerCase();
        String localizedName = plugin.getConfigManager().getLocalizedMessage("server_messages", key, player);
        return localizedName != null ? localizedName : gamemode.name().toLowerCase();
    }

    /**
     * Checks if the submodule is enabled.
     */
    public boolean isEnabled() {
        return gamemodeMessageEnabled;
    }

    /**
     * Enables/disables the submodule.
     */
    public void setEnabled(boolean enabled) {
        this.gamemodeMessageEnabled = enabled;
    }
}