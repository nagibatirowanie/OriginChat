package me.nagibatirowanie.originchat.translate;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.database.DatabaseHelper;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Менеджер автоперевода сообщений для игроков
 */
public class TranslateManager {

    private final OriginChat plugin;
    private final Map<UUID, Boolean> playerTranslateSettings;
    private final DatabaseHelper dbHelper;
    private static final String TRANSLATE_TABLE = "translate_settings";

    public TranslateManager(OriginChat plugin) {
        this.plugin = plugin;
        this.playerTranslateSettings = new HashMap<>();
        this.dbHelper = new DatabaseHelper(plugin);
        initDatabase();
        loadFromDatabase();
    }

    /**
     * Инициализирует таблицу в базе данных для хранения настроек автоперевода
     */
    private void initDatabase() {
        // Проверяем, что DatabaseManager существует и соединение активно
        if (plugin.getDatabaseManager() == null) {
            plugin.getPluginLogger().warning("[TranslateManager] DatabaseManager не инициализирован, настройки автоперевода будут храниться только в памяти");
            return;
        }
        
        // Проверяем состояние соединения с базой данных
        try {
            // Пробуем получить соединение для проверки его работоспособности
            if (plugin.getDatabaseManager().getConnection() == null) {
                plugin.getPluginLogger().warning("[TranslateManager] Не удалось получить соединение с базой данных, настройки автоперевода будут храниться только в памяти");
                return;
            }
        } catch (Exception e) {
            plugin.getPluginLogger().warning("[TranslateManager] Ошибка при проверке соединения с базой данных: " + e.getMessage());
            return;
        }
        
        // Если соединение активно, но isEnabled() возвращает false, выводим предупреждение
        if (!plugin.getDatabaseManager().isEnabled()) {
            plugin.getPluginLogger().warning("[TranslateManager] DatabaseManager.isEnabled() вернул false, но соединение активно. Продолжаем инициализацию.");
        }

        // Создаем таблицу для настроек автоперевода, если она не существует
        if (!dbHelper.tableExists(TRANSLATE_TABLE)) {
            try {
                plugin.getPluginLogger().info("[TranslateManager] Попытка создания таблицы " + TRANSLATE_TABLE + " с полем player_uuid VARCHAR(36)");
                boolean created = dbHelper.createTable(TRANSLATE_TABLE, "player_uuid", "VARCHAR(36)", false);
                if (created) {
                    plugin.getPluginLogger().info("[TranslateManager] Таблица создана успешно, добавляем колонку enabled");
                    boolean columnAdded = dbHelper.addColumn(TRANSLATE_TABLE, "enabled", "INTEGER", "0");
                    if (columnAdded) {
                        plugin.getPluginLogger().info("[TranslateManager] Таблица для настроек автоперевода успешно создана и инициализирована");
                    } else {
                        plugin.getPluginLogger().severe("[TranslateManager] Ошибка при добавлении колонки enabled в таблицу " + TRANSLATE_TABLE);
                    }
                } else {
                    plugin.getPluginLogger().severe("[TranslateManager] Не удалось создать таблицу " + TRANSLATE_TABLE + ". Проверьте права доступа и состояние базы данных.");
                }
            } catch (Exception e) {
                plugin.getPluginLogger().severe("[TranslateManager] Критическая ошибка при создании таблицы " + TRANSLATE_TABLE + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Загружает настройки автоперевода из базы данных
     */
    public void loadFromDatabase() {
        if (!plugin.getDatabaseManager().isEnabled()) {
            return;
        }

        playerTranslateSettings.clear();
        
        // Получаем все записи из таблицы настроек автоперевода
        java.util.List<String> columns = java.util.Arrays.asList("player_uuid", "enabled");
        java.util.List<Map<String, Object>> results = dbHelper.getData(TRANSLATE_TABLE, null, null, columns);
        
        for (Map<String, Object> row : results) {
            try {
                String uuidStr = (String) row.get("player_uuid");
                Object enabledObj = row.get("enabled");
                boolean enabled = false;
                
                if (enabledObj instanceof Integer) {
                    enabled = ((Integer) enabledObj) == 1;
                } else if (enabledObj instanceof Boolean) {
                    enabled = (Boolean) enabledObj;
                }
                
                if (uuidStr != null) {
                    UUID uuid = UUID.fromString(uuidStr);
                    playerTranslateSettings.put(uuid, enabled);
                }
            } catch (Exception e) {
                plugin.getPluginLogger().warning("Ошибка при загрузке настроек автоперевода из базы данных: " + e.getMessage());
            }
        }

        plugin.getPluginLogger().info("Загружены настройки автоперевода для " + playerTranslateSettings.size() + " игроков из базы данных");
    }

    /**
     * Сохраняет настройку автоперевода для игрока в базу данных
     * @param playerUuid UUID игрока
     * @param enabled состояние настройки
     */
    private void saveToDatabase(UUID playerUuid, boolean enabled) {
        if (!plugin.getDatabaseManager().isEnabled()) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("enabled", enabled ? 1 : 0);
        
        dbHelper.saveData(TRANSLATE_TABLE, "player_uuid", playerUuid.toString(), data);
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
        saveToDatabase(player.getUniqueId(), enabled);
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