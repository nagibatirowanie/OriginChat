package me.nagibatirowanie.originchat.module.modules;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.module.AbstractModule;
import me.nagibatirowanie.originchat.utils.FormatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Locale;

/**
 * Module to display localized death messages with optional sound effects.
 */
public class LocaleDeathsModule extends AbstractModule implements Listener {

    private boolean disableVanillaMessages;
    private boolean playSound;
    private String deathSound;
    private String prefix;
    private String suffix;
    private boolean registered;

    public LocaleDeathsModule(OriginChat plugin) {
        super(plugin, "locale_deaths", "Localized Death Messages",
              "Displays localized death messages with sound effects", "1.0");
    }

    @Override
    public void onEnable() {
        loadModuleConfig("modules/locale_deaths");
        if (config == null) {
            config = plugin.getConfigManager().getMainConfig();
        }
        loadConfig();
        if (!registered) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
    }

    @Override
    public void onDisable() {
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
    }

    /**
     * Loads death message settings from configuration.
     */
    private void loadConfig() {
        try {
            ConfigurationSection section = config.getConfigurationSection("death_messages");
            if (section == null) {
                section = config.createSection("death_messages");
            }

            disableVanillaMessages = section.getBoolean("disable-vanilla-messages", true);
            playSound = section.getBoolean("play_sound", true);
            deathSound = section.getString("death_sound", "ENTITY_CAT_DEATH");
            prefix = section.getString("prefix", "");
            suffix = section.getString("suffix", "");
        } catch (Exception e) {
            log("Error loading locale deaths config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles player death events to send localized messages and play sounds.
     *
     * @param event the player death event
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deceased = event.getEntity();
        Component original = event.deathMessage();

        if (disableVanillaMessages) {
            event.deathMessage(null);
        }

        if (original == null) {
            return;
        }

        for (Player recipient : plugin.getServer().getOnlinePlayers()) {
            Component localized = GlobalTranslator.render(original, recipient.locale());
            Component formatted = formatMessage(localized);
            recipient.sendMessage(formatted);
            if (playSound) {
                playSound(recipient, deceased);
            }
        }
    }

    /**
     * Applies prefix and suffix to a message component.
     *
     * @param message the original message
     * @return the message with prefix and suffix applied
     */
    private Component formatMessage(Component message) {
        Component pre = FormatUtil.format(prefix);
        Component suf = FormatUtil.format(suffix);
        return pre.append(message).append(suf);
    }

    /**
     * Plays the configured death sound to a player at the deceased's location.
     *
     * @param player the recipient of the sound
     * @param deceased the player who died
     */
    private void playSound(Player player, Player deceased) {
        try {
            Sound sound = Sound.valueOf(deathSound.toUpperCase(Locale.ROOT));
            player.playSound(deceased.getLocation(), sound, 1.0F, 1.0F);
        } catch (IllegalArgumentException e) {
            plugin.getPluginLogger().warning("Invalid death sound: " + deathSound);
        }
    }
}