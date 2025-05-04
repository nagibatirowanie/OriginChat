package me.nagibatirowanie.originChat.Utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Locale;

import static me.nagibatirowanie.originLib.Messages.replaceHexColors;

public class LocaleDeaths implements Listener {

    private final Plugin plugin;
    private boolean enabled;
    private boolean disableVanillaMessages;
    private boolean playSound;
    private String deathSound;
    private String prefix;
    private String suffix;
    private FileConfiguration config;

    public LocaleDeaths(Plugin plugin) {
        this.plugin = plugin;
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            plugin.getLogger().info("Death messages module enabled");
            //this.enabled = plugin.getConfig().getBoolean("death_messages.enable");
            loadConfig();
        }
    }

    private void loadConfig() {
        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                plugin.saveResource("config.yml", false);
            }
            config = YamlConfiguration.loadConfiguration(configFile);

            disableVanillaMessages = config.getBoolean("death_messages.disable-vanilla-messages", true);
            playSound = config.getBoolean("death_messages.play_sound", true);
            deathSound = config.getString("death_messages.death_sound", "ENTITY_CAT_DEATH");
            prefix = config.getString("death_messages.prefix", "");
            suffix = config.getString("death_messages.suffix", "");
            enabled = config.getBoolean("enabled", true);
        } catch (Exception e) {
            plugin.getLogger().severe("Config loading failed: " + e.getMessage());
        }
    }

    public void reloadConfig() {
        loadConfig();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (enabled) {

        Player deceased = event.getEntity();
        Component originalMessage = event.deathMessage();

        if (disableVanillaMessages) {
            event.deathMessage(null);
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Component translatedMessage = getLocalizedMessage(originalMessage, player);
            Component finalMessage = formatMessage(translatedMessage);

            player.sendMessage(finalMessage);

            if (playSound) {
                playDeathSound(player, deceased);
            }
        }
        }else{
            if (disableVanillaMessages) {
                event.deathMessage(null);
            }
        }
    }

    private Component getLocalizedMessage(Component originalMessage, Player player) {
        Locale locale = player.locale();
        return GlobalTranslator.render(originalMessage, locale);
    }

    private Component formatMessage(Component message) {
        String formattedPrefix = ChatColor.translateAlternateColorCodes('&', replaceHexColors(prefix));
        String formattedSuffix = ChatColor.translateAlternateColorCodes('&', replaceHexColors(suffix));

        return Component.text(formattedPrefix)
                .append(message)
                .append(Component.text(formattedSuffix));
    }

    private void playDeathSound(Player player, Player deceased) {
        try {
            Sound sound = Sound.valueOf(deathSound.toUpperCase());
            player.playSound(deceased.getLocation(), sound, 1.0F, 1.0F);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid death sound: " + deathSound);
        }
    }
}