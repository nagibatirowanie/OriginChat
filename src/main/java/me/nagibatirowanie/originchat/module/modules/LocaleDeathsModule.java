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
import me.nagibatirowanie.originchat.module.AbstractModule;
import me.nagibatirowanie.originchat.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Locale;

/**
 * Module for localized death messages
 */
public class LocaleDeathsModule extends AbstractModule implements Listener {

    private boolean disableVanillaMessages;
    private boolean playSound;
    private String deathSound;
    private String prefix;
    private String suffix;
    private boolean registered = false;

    public LocaleDeathsModule(OriginChat plugin) {
        super(plugin, "locale_deaths", "Localized Death Messages",
              "Module for displaying localized death messages for players", "1.0");
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

    // Load settings from config
    private void loadConfig() {
        try {
            ConfigurationSection deathMessagesConfig = config.getConfigurationSection("death_messages");
            if (deathMessagesConfig == null) deathMessagesConfig = config.createSection("death_messages");

            disableVanillaMessages = deathMessagesConfig.getBoolean("disable-vanilla-messages", true);
            playSound = deathMessagesConfig.getBoolean("play_sound", true);
            deathSound = deathMessagesConfig.getString("death_sound", "ENTITY_CAT_DEATH");
            prefix = deathMessagesConfig.getString("prefix", "");
            suffix = deathMessagesConfig.getString("suffix", "");
            debug("LocaleDeathsModule configuration loaded.");
        } catch (Exception e) {
            log("❗ Error when loading LocalDeaths configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        debug("PlayerDeathEvent triggered.");

        Player deceased = event.getEntity();
        Component originalMessage = event.deathMessage();

        if (disableVanillaMessages) {
            event.deathMessage(null);
        }

        if (originalMessage == null) {
            debug("Original death message is null.");
            return;
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Component translatedMessage = getLocalizedMessage(originalMessage, player);
            Component finalMessage = formatMessage(translatedMessage);

            player.sendMessage(finalMessage);

            if (playSound) {
                playDeathSound(player, deceased);
            }
        }
    }

    // Get localized message for player
    private Component getLocalizedMessage(Component originalMessage, Player player) {
        Locale locale = player.locale();
        return GlobalTranslator.render(originalMessage, locale);
    }

    // Format message with prefix and suffix
    private Component formatMessage(Component message) {
        Component prefixComponent = ColorUtil.toComponent(prefix);
        Component suffixComponent = ColorUtil.toComponent(suffix);
        return prefixComponent
                .append(message)
                .append(suffixComponent);
    }

    // Play death sound for player
    private void playDeathSound(Player player, Player deceased) {
        try {
            Sound sound = Sound.valueOf(deathSound.toUpperCase());
            player.playSound(deceased.getLocation(), sound, 1.0F, 1.0F);
        } catch (IllegalArgumentException e) {
            plugin.getPluginLogger().warning("Invalid death sound: " + deathSound);
        }
    }
}