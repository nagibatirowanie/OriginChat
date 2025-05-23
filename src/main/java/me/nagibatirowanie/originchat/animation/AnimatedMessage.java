package me.nagibatirowanie.originchat.animation;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.utils.FormatUtil;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Class for managing animated chat messages
 */
public class AnimatedMessage {

    private static final Map<UUID, AnimatedMessage> activeAnimations = new HashMap<>();
    private static final Object animationLock = new Object(); // Object for synchronization
    
    private final OriginChat plugin;
    private final Player player;
    private final Animation animation;
    private BukkitTask task;
    private int currentFrame = 0;
    
    /**
     * Creates a new animated message for a player
     * @param plugin instance of the plugin
     * @param player the player to receive the message
     * @param animation the animation to display
     */
    public AnimatedMessage(OriginChat plugin, Player player, Animation animation) {
        this.plugin = plugin;
        this.player = player;
        this.animation = animation;
    }
    
    /**
     * Starts displaying the animated message
     * @param duration display duration in seconds (0 for infinite)
     */
    public void start(int duration) {
        try {
            // Stop the previous animation for this player, if it exists
            stop(player.getUniqueId());
            
            // Save this animation as the active one for this player
            synchronized (animationLock) {
                activeAnimations.put(player.getUniqueId(), this);
            }
            
            // Start the task to update the message
            task = new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        // Get the player's language
                        String locale = Animation.DEFAULT_LOCALE;
                        if (player != null && player.isOnline()) {
                            // Retrieve language from LocaleManager
                            locale = plugin.getLocaleManager().getPlayerLocale(player);
                            // Check if frames exist for this locale
                            if (!animation.hasLocale(locale)) {
                                locale = Animation.DEFAULT_LOCALE; // Fallback to default locale if no translation
                            }
                        }
                        
                        // Get the current frame for the player's language and send it
                        String frame = animation.getCurrentFrame(locale);
                        // Replace the player placeholder if present
                        if (frame != null && frame.contains("{player}")) {
                            frame = frame.replace("{player}", player.getName());
                        }
                        if (player != null && player.isOnline()) {
                            player.sendMessage(FormatUtil.format(player, frame, true, true, true));
                        } else {
                            // If the player is offline, stop the animation
                            stop(player.getUniqueId());
                        }
                    } catch (Exception e) {
                        plugin.getPluginLogger().warning("Error displaying animation: " + e.getMessage());
                        stop(player.getUniqueId()); // Stop animation on error
                    }
                }
            }.runTaskTimer(plugin, 0, animation.getInterval());
            
            // If a duration is specified, stop the animation after the given time
            if (duration > 0) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        stop(player.getUniqueId());
                    }
                }.runTaskLater(plugin, duration * 20L); // Convert seconds to ticks (20 ticks = 1 second)
            }
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Error starting animation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Stops the animated message for the specified player
     * @param playerId Player UUID
     */
    public static void stop(UUID playerId) {
        if (playerId == null) return;
        
        try {
            AnimatedMessage animation;
            synchronized (animationLock) {
                animation = activeAnimations.get(playerId);
                if (animation != null && animation.task != null) {
                    animation.task.cancel();
                    activeAnimations.remove(playerId);
                }
            }
        } catch (Exception e) {
            OriginChat.getInstance().getPluginLogger().warning("Error stopping animation: " + e.getMessage());
        }
    }
    
    /**
     * Stops all active animated messages
     */
    public static void stopAll() {
        try {
            synchronized (animationLock) {
                for (AnimatedMessage animation : activeAnimations.values()) {
                    if (animation != null && animation.task != null) {
                        animation.task.cancel();
                    }
                }
                activeAnimations.clear();
            }
        } catch (Exception e) {
            OriginChat.getInstance().getPluginLogger().severe("Error stopping all animations: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Checks if there is an active animation for the specified player
     * @param playerId Player UUID
     * @return true if the player has an active animation
     */
    public static boolean hasActiveAnimation(UUID playerId) {
        if (playerId == null) return false;
        
        synchronized (animationLock) {
            return activeAnimations.containsKey(playerId);
        }
    }
}