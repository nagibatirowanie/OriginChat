package me.nagibatirowanie.originchat.translate;

import me.nagibatirowanie.originchat.OriginChat;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Менеджер автоперевода сообщений для игроков
 */
public class TranslateManager {

    private final OriginChat plugin;
    private final Map<UUID, Boolean> playerTranslateSettings;
    private final File configFile;
    private FileConfiguration config;

    public TranslateManager(OriginChat plugin) {
        this.plugin = plugin;
        this.playerTranslateSettings = new HashMap<>();
        this.configFile = new File(plugin.getDataFolder(), "translate_settings.yml");
        loadConfig();
    }

    /**
     * Загружает настройки автоперевода из файла
     */
    public void loadConfig() {
        if (!configFile.exists()) {
            try {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
                plugin.getPluginLogger().info("Создан новый файл настроек автоперевода");
            } catch (IOException e) {
                plugin.getPluginLogger().severe("Не удалось создать файл настроек автоперевода: " + e.getMessage());
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        playerTranslateSettings.clear();

        if (config.contains("players")) {
            for (String uuidStr : config.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    boolean enabled = config.getBoolean("players." + uuidStr);
                    playerTranslateSettings.put(uuid, enabled);
                } catch (IllegalArgumentException e) {
                    plugin.getPluginLogger().warning("Некорректный UUID в файле настроек автоперевода: " + uuidStr);
                }
            }
        }

        plugin.getPluginLogger().info("Загружены настройки автоперевода для " + playerTranslateSettings.size() + " игроков");
    }

    /**
     * Сохраняет настройки автоперевода в файл
     */
    public void saveConfig() {
        if (config == null) {
            return;
        }

        try {
            if (!config.contains("players")) {
                config.createSection("players");
            }

            for (Map.Entry<UUID, Boolean> entry : playerTranslateSettings.entrySet()) {
                config.set("players." + entry.getKey().toString(), entry.getValue());
            }

            config.save(configFile);
        } catch (IOException e) {
            plugin.getPluginLogger().severe("Не удалось сохранить файл настроек автоперевода: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Проверяет, включен ли автоперевод для игрока
     * @param player игрок
     * @return true если автоперевод включен, false если выключен
     */
    public boolean isTranslateEnabled(Player player) {
        if (player == null) {
            plugin.getPluginLogger().info("[TranslateManager] isTranslateEnabled: player is null, returning false");
            return false;
        }
        boolean enabled = playerTranslateSettings.getOrDefault(player.getUniqueId(), false);
        plugin.getPluginLogger().info("[TranslateManager] isTranslateEnabled для игрока " + player.getName() + ": " + enabled);
        return enabled;
    }

    /**
     * Включает или выключает автоперевод для игрока
     * @param player игрок
     * @param enabled true для включения, false для выключения
     * @return новое состояние настройки
     */
    public boolean setTranslateEnabled(Player player, boolean enabled) {
        if (player == null) {
            plugin.getPluginLogger().info("[TranslateManager] setTranslateEnabled: player is null, returning false");
            return false;
        }
        
        plugin.getPluginLogger().info("[TranslateManager] Устанавливаем автоперевод для игрока " + player.getName() + ": " + enabled);
        playerTranslateSettings.put(player.getUniqueId(), enabled);
        saveConfig();
        return enabled;
    }

    /**
     * Переключает состояние автоперевода для игрока
     * @param player игрок
     * @return новое состояние настройки
     */
    public boolean toggleTranslate(Player player) {
        plugin.getPluginLogger().info("[TranslateManager] Переключаем автоперевод для игрока " + player.getName());
        boolean currentState = isTranslateEnabled(player);
        boolean newState = !currentState;
        plugin.getPluginLogger().info("[TranslateManager] Текущее состояние: " + currentState + ", новое состояние: " + newState);
        setTranslateEnabled(player, newState);
        return newState;
    }

    /**
     * Пример использования перевода с расширенным логированием ошибок
     * @param player игрок
     * @param text текст для перевода
     * @param toLang целевой язык
     * @return переведённый текст или null при ошибке
     */
    public String tryTranslateWithLogging(Player player, String text, String toLang) {
        try {
            // Здесь предполагается вызов TranslateUtil.translate или translateAsync
            String translated = me.nagibatirowanie.originchat.utils.TranslateUtil.translate(text, toLang);
            plugin.getPluginLogger().info(String.format("[TranslateManager] Перевод для игрока %s (%s) на язык %s выполнен успешно.", player != null ? player.getName() : "null", player != null ? player.getUniqueId() : "null", toLang));
            return translated;
        } catch (Exception e) {
            plugin.getPluginLogger().severe(String.format("[TranslateManager] Ошибка перевода для игрока %s (%s) на язык %s. Текст: '%s'. Причина: %s", player != null ? player.getName() : "null", player != null ? player.getUniqueId() : "null", toLang, text, e.getMessage()));
            e.printStackTrace();
            return null;
        }
    }
}