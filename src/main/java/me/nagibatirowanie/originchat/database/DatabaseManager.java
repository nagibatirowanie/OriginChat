package me.nagibatirowanie.originchat.database;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.database.providers.MySQLProvider;
import me.nagibatirowanie.originchat.database.providers.PostgreSQLProvider;
import me.nagibatirowanie.originchat.database.providers.SQLiteProvider;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Менеджер базы данных плагина
 */
public class DatabaseManager {

    private final OriginChat plugin;
    private DatabaseProvider provider;
    private DatabaseCommands databaseCommands;

    public DatabaseManager(OriginChat plugin) {
        this.plugin = plugin;
        this.databaseCommands = new DatabaseCommands(plugin);
    }

    /**
     * Инициализировать менеджер базы данных
     */
    /**
     * Загружает конфигурацию базы данных из файла database.yml
     * @return конфигурация базы данных или null, если файл не найден
     */
    private FileConfiguration loadDatabaseConfig() {
        File databaseConfigFile = new File(plugin.getDataFolder(), "database.yml");
        
        if (!databaseConfigFile.exists()) {
            plugin.saveResource("database.yml", false);
            plugin.getPluginLogger().info("Создан файл конфигурации базы данных database.yml");
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(databaseConfigFile);
        
        // Проверяем и обновляем конфигурацию, если необходимо
        boolean updated = plugin.getConfigManager().getConfigUpdater().checkAndUpdateConfig(
                databaseConfigFile, config, "database.yml");
        
        if (updated) {
            config = YamlConfiguration.loadConfiguration(databaseConfigFile);
            plugin.getPluginLogger().info("Конфигурация базы данных была обновлена");
        }
        
        return config;
    }
    
    public void initialize() {
        // Загружаем конфигурацию базы данных из отдельного файла
        FileConfiguration dbConfig = loadDatabaseConfig();
        
        if (dbConfig == null) {
            plugin.getPluginLogger().severe("Не удалось загрузить конфигурацию базы данных");
            return;
        }
        
        // База данных всегда включена
        
        String type = dbConfig.getString("type", "sqlite").toLowerCase();
        plugin.getPluginLogger().info("Тип базы данных из конфигурации: " + type);
        
        try {
            switch (type) {
                case "sqlite":
                    String sqliteDatabase = dbConfig.getString("sqlite.database", "originchat");
                    int sqliteTimeout = dbConfig.getInt("sqlite.options.timeout", 30);
                    boolean sqliteAutoCreate = dbConfig.getBoolean("sqlite.options.auto-create", true);
                    
                    plugin.getPluginLogger().info("Подключение к SQLite базе данных: " + sqliteDatabase);
                    provider = new SQLiteProvider(plugin, sqliteDatabase);
                    break;
                    
                case "mysql":
                    String mysqlHost = dbConfig.getString("mysql.host", "localhost");
                    int mysqlPort = dbConfig.getInt("mysql.port", 3306);
                    String mysqlDatabase = dbConfig.getString("mysql.database", "originchat");
                    String mysqlUsername = dbConfig.getString("mysql.username", "root");
                    String mysqlPassword = dbConfig.getString("mysql.password", "password");
                    boolean mysqlUseSSL = dbConfig.getBoolean("mysql.use-ssl", false);
                    
                    // Дополнительные параметры подключения
                    int mysqlMaxPoolSize = dbConfig.getInt("mysql.options.max-pool-size", 10);
                    int mysqlConnectionTimeout = dbConfig.getInt("mysql.options.connection-timeout", 30000);
                    
                    plugin.getPluginLogger().info("Подключение к MySQL базе данных: " + mysqlHost + ":" + mysqlPort + "/" + mysqlDatabase);
                    provider = new MySQLProvider(plugin, mysqlHost, mysqlPort, mysqlDatabase, mysqlUsername, mysqlPassword, mysqlUseSSL);
                    break;
                    
                case "postgresql":
                    String pgHost = dbConfig.getString("postgresql.host", "localhost");
                    int pgPort = dbConfig.getInt("postgresql.port", 5432);
                    String pgDatabase = dbConfig.getString("postgresql.database", "originchat");
                    String pgUsername = dbConfig.getString("postgresql.username", "postgres");
                    String pgPassword = dbConfig.getString("postgresql.password", "password");
                    boolean pgUseSSL = dbConfig.getBoolean("postgresql.use-ssl", false);
                    String pgSchema = dbConfig.getString("postgresql.options.schema", "public");
                    
                    plugin.getPluginLogger().info("Подключение к PostgreSQL базе данных: " + pgHost + ":" + pgPort + "/" + pgDatabase);
                    provider = new PostgreSQLProvider(plugin, pgHost, pgPort, pgDatabase, pgUsername, pgPassword, pgUseSSL);
                    break;
                    
                default:
                    plugin.getPluginLogger().warning("Неизвестный тип базы данных: " + type + ". Используется SQLite.");
                    provider = new SQLiteProvider(plugin, "originchat");
                    break;
            }
            // Проверяем, нужно ли выполнить миграцию
            boolean autoMigrate = dbConfig.getBoolean("migration.auto-migrate", true);
            boolean backupBeforeMigrate = dbConfig.getBoolean("migration.backup-before-migrate", true);
            
            provider.initialize();
            
            if (autoMigrate) {
                plugin.getPluginLogger().info("Выполняется миграция базы данных...");
                if (backupBeforeMigrate) {
                    plugin.getPluginLogger().info("Создание резервной копии базы данных перед миграцией...");
                    // Здесь можно добавить код для создания резервной копии
                }
                provider.migrate();
            }
            
            plugin.getPluginLogger().info("База данных " + provider.getType() + " успешно инициализирована.");
            
            // Регистрируем команды для работы с базой данных после инициализации провайдера
            databaseCommands.registerCommands();
        } catch (SQLException e) {
            plugin.getPluginLogger().severe("Ошибка при инициализации базы данных: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Закрыть соединение с базой данных
     */
    public void close() {
        if (provider != null) {
            provider.close();
        }
        
        // Отменяем регистрацию команд
        databaseCommands.unregisterCommands();
    }

    /**
     * Получить соединение с базой данных
     * @return соединение с базой данных
     * @throws SQLException при ошибке соединения
     */
    public Connection getConnection() throws SQLException {
        if (provider == null) {
            throw new SQLException("База данных не инициализирована");
        }
        return provider.getConnection();
    }

    /**
     * Проверить, включена ли база данных
     * @return true, если база данных включена
     */
    public boolean isEnabled() {
        if (provider == null) {
            return false;
        }
        
        try {
            // Проверяем соединение более надежным способом
            Connection conn = provider.getConnection();
            if (conn == null || conn.isClosed()) {
                return false;
            }
            // Проверяем работоспособность соединения простым запросом
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT 1");
                return true;
            }
        } catch (SQLException e) {
            plugin.getPluginLogger().warning("Ошибка при проверке состояния соединения с базой данных: " + e.getMessage());
            return false;
        }
    }

    /**
     * Получить тип провайдера базы данных
     * @return тип провайдера
     */
    public String getProviderType() {
        if (provider == null) {
            // Если провайдер еще не инициализирован, получаем тип из конфигурации
            FileConfiguration dbConfig = loadDatabaseConfig();
            if (dbConfig != null) {
                return dbConfig.getString("type", "sqlite").toLowerCase();
            }
            return "none";
        }
        return provider.getType();
    }
    
    /**
     * Сохраняет изменения в конфигурации базы данных
     * @param config конфигурация для сохранения
     * @return true если сохранение прошло успешно
     */
    public boolean saveDatabaseConfig(FileConfiguration config) {
        if (config == null) {
            return false;
        }
        
        try {
            File databaseConfigFile = new File(plugin.getDataFolder(), "database.yml");
            config.save(databaseConfigFile);
            plugin.getPluginLogger().info("Конфигурация базы данных успешно сохранена");
            return true;
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Ошибка при сохранении конфигурации базы данных: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Получает текущую конфигурацию базы данных
     * @return текущая конфигурация базы данных
     */
    public FileConfiguration getDatabaseConfig() {
        return loadDatabaseConfig();
    }
    
    /**
     * Изменяет тип базы данных и перезагружает соединение
     * @param type новый тип базы данных (sqlite, mysql, postgresql)
     * @return true если изменение прошло успешно
     */
    public boolean changeDatabaseType(String type) {
        if (type == null || type.isEmpty()) {
            return false;
        }
        
        // Проверяем, поддерживается ли указанный тип
        if (!type.equalsIgnoreCase("sqlite") && 
            !type.equalsIgnoreCase("mysql") && 
            !type.equalsIgnoreCase("postgresql")) {
            plugin.getPluginLogger().warning("Неподдерживаемый тип базы данных: " + type);
            return false;
        }
        
        // Получаем текущую конфигурацию
        FileConfiguration config = getDatabaseConfig();
        if (config == null) {
            return false;
        }
        
        // Сохраняем новый тип
        config.set("type", type.toLowerCase());
        
        // Сохраняем изменения
        if (!saveDatabaseConfig(config)) {
            return false;
        }
        
        // Закрываем текущее соединение
        close();
        
        // Переинициализируем базу данных
        initialize();
        
        return isEnabled();
    }

    /**
     * Сохранить или обновить данные игрока
     * @param uuid UUID игрока
     * @param name имя игрока
     * @param locale локаль игрока
     * @param translateEnabled включен ли автоперевод
     */
    public void savePlayerData(UUID uuid, String name, String locale, boolean translateEnabled) {
        if (!isEnabled()) return;
        String sql = "INSERT INTO oc_players (uuid, name, locale, translate_enabled, last_join) " +
                "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON DUPLICATE KEY UPDATE name = ?, locale = ?, translate_enabled = ?, last_join = CURRENT_TIMESTAMP";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, locale);
            ps.setInt(4, translateEnabled ? 1 : 0);
            ps.setString(5, name);
            ps.setString(6, locale);
            ps.setInt(7, translateEnabled ? 1 : 0);
            ps.executeUpdate();
            plugin.getPluginLogger().info("Таблица настроек автоперевода успешно создана или обновлена (savePlayerData). SQL: " + sql);
        } catch (SQLException e) {
            plugin.getPluginLogger().severe("Не удалось создать или обновить таблицу для настроек автоперевода. Ошибка: " + e.getMessage() + ". SQL: " + sql);
            e.printStackTrace();
        }
    }

    /**
     * Получить локаль игрока из базы данных
     * @param uuid UUID игрока
     * @return локаль игрока или null, если игрок не найден
     */
    public String getPlayerLocale(UUID uuid) {
        if (!isEnabled()) return null;
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT locale FROM oc_players WHERE uuid = ?")) {
            
            ps.setString(1, uuid.toString());
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("locale");
                }
            }
        } catch (SQLException e) {
            plugin.getPluginLogger().warning("Ошибка при получении локали игрока: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Проверить, включен ли автоперевод для игрока
     * @param uuid UUID игрока
     * @return true, если автоперевод включен
     */
    public boolean isTranslateEnabled(UUID uuid) {
        if (!isEnabled()) return false;
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT translate_enabled FROM oc_players WHERE uuid = ?")) {
            
            ps.setString(1, uuid.toString());
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("translate_enabled") == 1;
                }
            }
        } catch (SQLException e) {
            plugin.getPluginLogger().warning("Ошибка при получении статуса автоперевода: " + e.getMessage());
        }
        
        return false;
    }

    /**
     * Установить статус автоперевода для игрока
     * @param uuid UUID игрока
     * @param enabled статус автоперевода
     */
    public void setTranslateEnabled(UUID uuid, boolean enabled) {
        if (!isEnabled()) return;
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE oc_players SET translate_enabled = ? WHERE uuid = ?")) {
            
            ps.setInt(1, enabled ? 1 : 0);
            ps.setString(2, uuid.toString());
            
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getPluginLogger().warning("Ошибка при установке статуса автоперевода: " + e.getMessage());
        }
    }
    
    /**
     * Сохранить данные игрока в базу данных
     * @param player игрок
     */
    public void savePlayerData(Player player) {
        databaseCommands.savePlayerData(player);
    }
    
    /**
     * Получить объект для работы с командами базы данных
     * @return объект DatabaseCommands
     */
    public DatabaseCommands getDatabaseCommands() {
        return databaseCommands;
    }
}