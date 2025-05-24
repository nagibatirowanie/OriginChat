package me.nagibatirowanie.originchat.module.modules.servermessages;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.utils.FormatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Submodule for handling seed command messages with localization support.
 */
public class SeedMessagesSubmodule implements Listener {
    private final OriginChat plugin;
    private final ServerMessagesModule parentModule;

    // Configuration flags
    private boolean seedMessageEnabled;
    private boolean disableVanillaMessages;
    
    // Pattern for seed command
    private static final Pattern SEED_PATTERN = Pattern.compile(
            "^/(seed|minecraft:seed)\\s*$", 
            Pattern.CASE_INSENSITIVE);

    public SeedMessagesSubmodule(OriginChat plugin, ServerMessagesModule parentModule) {
        this.plugin = plugin;
        this.parentModule = parentModule;
    }

    /**
     * Initializes the submodule.
     */
    public void initialize() {
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getPluginLogger().info("[ServerMessages] SeedMessagesSubmodule initialized.");
    }

    /**
     * Shuts down the submodule.
     */
    public void shutdown() {
        PlayerCommandPreprocessEvent.getHandlerList().unregister(this);
        ServerCommandEvent.getHandlerList().unregister(this);
        plugin.getPluginLogger().info("[ServerMessages] SeedMessagesSubmodule disabled.");
    }

    /**
     * Loads the submodule's configuration.
     */
    public void loadConfig() {
        seedMessageEnabled = parentModule.getConfig().getBoolean("seed_message_enabled", true);
        disableVanillaMessages = parentModule.getConfig().getBoolean("disable_vanilla_seed_messages", true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!seedMessageEnabled) return;

        String message = event.getMessage();
        Matcher matcher = SEED_PATTERN.matcher(message);
        
        if (matcher.matches()) {
            handleSeedCommand(event.getPlayer());
            event.setCancelled(disableVanillaMessages);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        if (!seedMessageEnabled) return;
        
        String message = event.getCommand();
        Matcher matcher = SEED_PATTERN.matcher("/" + message);
        
        if (matcher.matches()) {
            // Handle console seed command
            if (disableVanillaMessages) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    long seed = Bukkit.getWorlds().get(0).getSeed();
                    String formattedMessage = plugin.getConfigManager().getLocalizedMessage(
                            "server_messages", "seed_messages.console", "en");
                    if (formattedMessage != null) {
                        formattedMessage = formattedMessage.replace("{seed}", String.valueOf(seed));
                        Bukkit.getConsoleSender().sendMessage(FormatUtil.format(formattedMessage));
                    } else {
                        // Fallback message
                        Bukkit.getConsoleSender().sendMessage("Seed: " + seed);
                    }
                });
            }
        }
    }

    private void handleSeedCommand(Player player) {
        if (disableVanillaMessages) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                long seed = player.getWorld().getSeed();
                String formattedMessage = plugin.getConfigManager().getLocalizedMessage(
                        "server_messages", "seed_messages.player", player);
                if (formattedMessage != null) {
                    formattedMessage = formattedMessage.replace("{seed}", String.valueOf(seed));
                    player.sendMessage(FormatUtil.format(player, formattedMessage));
                } else {
                    // Fallback message
                    player.sendMessage("Seed: " + seed);
                }
                

            });
        }
    }

    /**
     * Checks if the submodule is enabled.
     */
    public boolean isEnabled() {
        return seedMessageEnabled;
    }

    /**
     * Enables/disables the submodule.
     */
    public void setEnabled(boolean enabled) {
        this.seedMessageEnabled = enabled;
    }
}