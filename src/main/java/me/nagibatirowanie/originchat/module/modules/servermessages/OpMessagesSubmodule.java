package me.nagibatirowanie.originchat.module.modules.servermessages;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.utils.FormatUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

/**
 * Submodule for handling operator permission grant and revoke messages.
 */
public class OpMessagesSubmodule implements Listener {
    private final OriginChat plugin;
    private final ServerMessagesModule parentModule;

    // Configuration flags
    private boolean opMessageEnabled;
    private boolean disableVanillaMessages;

    public OpMessagesSubmodule(OriginChat plugin, ServerMessagesModule parentModule) {
        this.plugin = plugin;
        this.parentModule = parentModule;
    }

    /**
     * Initializes the submodule.
     */
    public void initialize() {
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getPluginLogger().info("[ServerMessages] OpMessagesSubmodule initialized.");
    }

    /**
     * Shuts down the submodule.
     */
    public void shutdown() {
        PlayerCommandPreprocessEvent.getHandlerList().unregister(this);
        plugin.getPluginLogger().info("[ServerMessages] OpMessagesSubmodule disabled.");
    }

    /**
     * Loads the submodule's configuration.
     */
    public void loadConfig() {
        opMessageEnabled = parentModule.getConfig().getBoolean("op_message_enabled", true);
        disableVanillaMessages = parentModule.getConfig().getBoolean("disable_vanilla_op_messages", true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!opMessageEnabled) return;

        String message = event.getMessage();
        String lower = message.toLowerCase();

        // Handling /op command
        if (lower.startsWith("/op ") || lower.startsWith("/minecraft:op ")) {
            handleSubcommand(event, message, "op");
        }
        // Handling /deop command
        else if (lower.startsWith("/deop ") || lower.startsWith("/minecraft:deop ")) {
            handleSubcommand(event, message, "deop");
        }
    }

    private void handleSubcommand(PlayerCommandPreprocessEvent event, String message, String type) {
        String[] parts = message.split(" ", 2);
        if (parts.length < 2) return;
        final String rawTarget = parts[1].trim();
        final Player sender = event.getPlayer();
        final String commandType = type; // Creating a final copy of the 'type' variable

        if (disableVanillaMessages) {
            // Cancelling the original command
            event.setCancelled(true);

            // Executing the command via the server, but with additional logic
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Remembering the player's current operator status
                final OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(rawTarget);
                final boolean wasOpped = targetPlayer != null && targetPlayer.isOp();

                // Executing the command as console
                boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandType + " " + rawTarget);

                // Checking if the operator status has changed
                if (success) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        boolean isNowOpped = targetPlayer != null && targetPlayer.isOp();

                        // Checking if the command actually changed the operator status
                        boolean statusChanged = ("op".equals(commandType) && !wasOpped && isNowOpped) ||
                                               ("deop".equals(commandType) && wasOpped && !isNowOpped);

                        if (statusChanged) {
                            // Sending custom messages
                            if ("op".equals(commandType)) {
                                sendOpMessages(sender, rawTarget);
                            } else {
                                sendDeopMessages(sender, rawTarget);
                            }
                        } else {
                            // If the status did not change, sending the original error message
                            if ("op".equals(commandType) && isNowOpped) {
                                sender.sendMessage(rawTarget + " is already op");
                            } else if ("deop".equals(commandType) && !isNowOpped) {
                                sender.sendMessage(rawTarget + " is not op");
                            } else {
                                sender.sendMessage("Could not " + commandType + " " + rawTarget);
                            }
                        }
                    }, 2L);
                }
            });
        } else {
            // If standard messages are not disabled, just send our additional ones
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if ("op".equals(commandType)) {
                    sendOpMessages(sender, rawTarget);
                } else {
                    sendDeopMessages(sender, rawTarget);
                }
            }, 2L);
        }
    }

    private void sendOpMessages(Player sender, String targetName) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
        Player target = (offline.isOnline() ? Bukkit.getPlayer(targetName) : null);

        // 1) Message to the sender
        List<String> senderMsgs = plugin.getConfigManager().getLocalizedMessageList(
                "server_messages", "op_messages.sender", sender);
        if (!senderMsgs.isEmpty()) {
            sender.sendMessage(FormatUtil.format(sender,
                    senderMsgs.get(0).replace("{target}", targetName)));
        }

        // 2) Message to the target
        if (target != null && target.isOp()) {
            List<String> targetMsgs = plugin.getConfigManager().getLocalizedMessageList(
                    "server_messages", "op_messages.target", target);
            if (!targetMsgs.isEmpty()) {
                target.sendMessage(FormatUtil.format(target,
                        targetMsgs.get(0).replace("{sender}", sender.getName())));
            }

            // 3) Broadcast to all ops
            String broadcast = plugin.getConfigManager().getLocalizedMessage(
                    "server_messages", "op_messages.broadcast", sender);
            if (broadcast != null) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.isOp() && !p.equals(sender) && !p.equals(target)) {
                        p.sendMessage(FormatUtil.format(p,
                                broadcast.replace("{sender}", sender.getName())
                                         .replace("{target}", targetName)));
                    }
                }
            }
        }
    }

    private void sendDeopMessages(Player sender, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);

        // 1) Message to the sender
        List<String> senderMsgs = plugin.getConfigManager().getLocalizedMessageList(
                "server_messages", "deop_messages.sender", sender);
        if (!senderMsgs.isEmpty()) {
            sender.sendMessage(FormatUtil.format(sender,
                    senderMsgs.get(0).replace("{target}", targetName)));
        }

        // 2) Message to the target
        if (target != null && !target.isOp()) {
            List<String> targetMsgs = plugin.getConfigManager().getLocalizedMessageList(
                    "server_messages", "deop_messages.target", target);
            if (!targetMsgs.isEmpty()) {
                target.sendMessage(FormatUtil.format(target,
                        targetMsgs.get(0).replace("{sender}", sender.getName())));
            }

            // 3) Broadcast to all ops
            String broadcast = plugin.getConfigManager().getLocalizedMessage(
                    "server_messages", "deop_messages.broadcast", sender);
            if (broadcast != null) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.isOp() && !p.equals(sender)) {
                        p.sendMessage(FormatUtil.format(p,
                                broadcast.replace("{sender}", sender.getName())
                                         .replace("{target}", targetName)));
                    }
                }
            }
        }
    }

    /**
     * Checks if the submodule is enabled.
     */
    public boolean isEnabled() {
        return opMessageEnabled;
    }

    /**
     * Enables/disables the submodule.
     */
    public void setEnabled(boolean enabled) {
        this.opMessageEnabled = enabled;
    }
}
