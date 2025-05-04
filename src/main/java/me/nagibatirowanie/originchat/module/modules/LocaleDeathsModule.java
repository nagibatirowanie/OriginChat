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
 * Модуль для локализованных сообщений о смерти
 */
public class LocaleDeathsModule extends AbstractModule implements Listener {

    private boolean disableVanillaMessages;
    private boolean playSound;
    private String deathSound;
    private String prefix;
    private String suffix;
    private boolean registered = false;

    public LocaleDeathsModule(OriginChat plugin) {
        super(plugin, "locale_deaths", "Локализованные сообщения о смерти", 
              "Модуль для отображения локализованных сообщений о смерти игроков", "1.0");
    }

    @Override
    public void onEnable() {
        // Загрузка конфигурации
        loadModuleConfig("modules/locale_deaths");
        if (config == null) {
            config = plugin.getConfigManager().getMainConfig();
        }
        loadConfig();
        
        if (!registered) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        log("Модуль локализованных сообщений о смерти успешно загружен.");
    }

    @Override
    public void onDisable() {
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
        log("Модуль локализованных сообщений о смерти выключен.");
    }

    private void loadConfig() {
        ConfigurationSection deathMessagesConfig = config.getConfigurationSection("death_messages");
        if (deathMessagesConfig == null) deathMessagesConfig = config.createSection("death_messages");

        disableVanillaMessages = deathMessagesConfig.getBoolean("disable-vanilla-messages", true);
        playSound = deathMessagesConfig.getBoolean("play_sound", true);
        deathSound = deathMessagesConfig.getString("death_sound", "ENTITY_CAT_DEATH");
        prefix = deathMessagesConfig.getString("prefix", "");
        suffix = deathMessagesConfig.getString("suffix", "");
        
        debug("Конфигурация модуля локализованных сообщений о смерти загружена.");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        debug("Событие PlayerDeathEvent вызвано.");

        Player deceased = event.getEntity();
        Component originalMessage = event.deathMessage();

        if (disableVanillaMessages) {
            event.deathMessage(null);
        }

        if (originalMessage == null) {
            debug("Оригинальное сообщение о смерти отсутствует.");
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

    private Component getLocalizedMessage(Component originalMessage, Player player) {
        Locale locale = player.locale();
        return GlobalTranslator.render(originalMessage, locale);
    }

    private Component formatMessage(Component message) {
        // Преобразуем prefix и suffix в компоненты с помощью ColorUtil.toComponent
        Component prefixComponent = ColorUtil.toComponent(prefix);
        Component suffixComponent = ColorUtil.toComponent(suffix);

        return prefixComponent
                .append(message)
                .append(suffixComponent);
    }

    private void playDeathSound(Player player, Player deceased) {
        try {
            Sound sound = Sound.valueOf(deathSound.toUpperCase());
            player.playSound(deceased.getLocation(), sound, 1.0F, 1.0F);
        } catch (IllegalArgumentException e) {
            plugin.getPluginLogger().warning("Неверный звук смерти: " + deathSound);
        }
    }
}