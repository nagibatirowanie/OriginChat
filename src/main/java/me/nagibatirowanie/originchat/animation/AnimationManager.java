package me.nagibatirowanie.originchat.animation;

import me.nagibatirowanie.originchat.OriginChat;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manager for animated text messages.
 * Supports placeholders like {animation_animationName}.
 */
public class AnimationManager {

    private final OriginChat plugin;
    private final Map<String, Animation> animations = new HashMap<>();
    private final Pattern animationPattern = Pattern.compile("\\{animation_([^}]+)\\}");
    private FileConfiguration animationsConfig;
    private BukkitTask animationTask;
    private final Object animationLock = new Object();

    public AnimationManager(OriginChat plugin) {
        this.plugin = plugin;

        try {
            loadAnimationsConfig();
            loadAnimations();
            startAnimationTask();
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Error initializing AnimationManager: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads animation configuration from animations.yml
     */
    private void loadAnimationsConfig() {
        File configFile = new File(plugin.getDataFolder(), "animations.yml");

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource("animations.yml", false);
        }

        animationsConfig = YamlConfiguration.loadConfiguration(configFile);

        try {
            List<String> ignoredSections = new ArrayList<>();

            com.tchristofferson.configupdater.ConfigUpdater.update(plugin, "animations.yml", configFile, ignoredSections);

            animationsConfig = YamlConfiguration.loadConfiguration(configFile);
            plugin.getPluginLogger().info("Animation configuration has been updated.");
        } catch (IOException e) {
            plugin.getPluginLogger().severe("Failed to update animation configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads animations from configuration
     */
    public void loadAnimations() {
        synchronized (animationLock) {
            animations.clear();

            if (animationsConfig == null) {
                plugin.getPluginLogger().warning("Failed to load animations: configuration not initialized.");
                return;
            }

            try {
                for (String key : animationsConfig.getKeys(false)) {
                    ConfigurationSection section = animationsConfig.getConfigurationSection(key);
                    if (section == null) continue;

                    int interval = section.getInt("interval", 20);

                    ConfigurationSection framesSection = section.getConfigurationSection("frames");
                    Animation animation;

                    if (framesSection != null) {
                        Map<String, List<String>> localizedFrames = new HashMap<>();
                        boolean hasFrames = false;

                        for (String locale : framesSection.getKeys(false)) {
                            List<String> localeFrames = framesSection.getStringList(locale);
                            if (!localeFrames.isEmpty()) {
                                localizedFrames.put(locale, localeFrames);
                                hasFrames = true;
                            }
                        }

                        if (!hasFrames) {
                            plugin.getPluginLogger().warning("Animation '" + key + "' contains no frames and will be skipped.");
                            continue;
                        }

                        animation = new Animation(key, interval, localizedFrames);
                        plugin.getPluginLogger().info("Loaded animation '" + key + "' with " + localizedFrames.size() + " locales and interval " + interval + " ticks.");
                    } else {
                        List<String> frames = section.getStringList("frames");

                        if (frames.isEmpty()) {
                            plugin.getPluginLogger().warning("Animation '" + key + "' contains no frames and will be skipped.");
                            continue;
                        }

                        animation = new Animation(key, interval, frames);
                        plugin.getPluginLogger().info("Loaded animation '" + key + "' with " + frames.size() + " frames and interval " + interval + " ticks.");
                    }

                    animations.put(key, animation);
                }

                plugin.getPluginLogger().info("Loaded " + animations.size() + " animations.");
            } catch (Exception e) {
                plugin.getPluginLogger().severe("Error loading animations: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Starts the animation frame update task
     */
    private void startAnimationTask() {
        try {
            if (animationTask != null && !animationTask.isCancelled()) {
                animationTask.cancel();
            }

            animationTask = new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        synchronized (animationLock) {
                            for (Animation animation : animations.values()) {
                                if (animation != null) {
                                    animation.nextFrame();
                                }
                            }
                        }
                    } catch (Exception e) {
                        plugin.getPluginLogger().severe("Error updating animation frames: " + e.getMessage());
                    }
                }
            }.runTaskTimer(plugin, 1L, 1L);
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Error starting animation task: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Stops the animation update task
     */
    public void stopAnimationTask() {
        try {
            if (animationTask != null && !animationTask.isCancelled()) {
                animationTask.cancel();
                animationTask = null;
            }

            AnimatedMessage.stopAll();
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Error stopping animation task: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Stops animated messages for a specific player
     * @param player the player
     */
    public void stopPlayerAnimations(Player player) {
        if (player != null) {
            AnimatedMessage.stop(player.getUniqueId());
        }
    }

    /**
     * Reloads animations from the configuration
     */
    public void reloadAnimations() {
        loadAnimationsConfig();
        loadAnimations();
        startAnimationTask();
    }

    /**
     * Replaces animation placeholders in the text with current frames
     * @param text the input text
     * @param player the player (can be null)
     * @return processed text with animation frames
     */
    public String processAnimations(String text, Player player) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        try {
            Matcher matcher = animationPattern.matcher(text);
            StringBuffer result = new StringBuffer();

            while (matcher.find()) {
                String animationName = matcher.group(1);
                Animation animation;

                synchronized (animationLock) {
                    animation = animations.get(animationName);
                }

                String replacement = "";
                if (animation != null) {
                    String locale = Animation.DEFAULT_LOCALE;
                    if (player != null) {
                        locale = plugin.getLocaleManager().getPlayerLocale(player);
                        if (!animation.hasLocale(locale)) {
                            locale = Animation.DEFAULT_LOCALE;
                        }
                    }

                    replacement = animation.getCurrentFrame(locale);
                    if (player != null && replacement != null && replacement.contains("{player}")) {
                        replacement = replacement.replace("{player}", player.getName());
                    }
                } else {
                    replacement = "[Animation '" + animationName + "' not found]";
                }

                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }

            matcher.appendTail(result);
            return result.toString();
        } catch (Exception e) {
            plugin.getPluginLogger().warning("Error processing animation placeholders: " + e.getMessage());
            return text;
        }
    }

    /**
     * Returns a list of all loaded animation names
     * @return list of animation names
     */
    public List<String> getAnimationNames() {
        synchronized (animationLock) {
            return new ArrayList<>(animations.keySet());
        }
    }

    /**
     * Gets the animation by name
     * @param name animation name
     * @return animation object or null if not found
     */
    public Animation getAnimation(String name) {
        synchronized (animationLock) {
            return animations.get(name);
        }
    }
}
