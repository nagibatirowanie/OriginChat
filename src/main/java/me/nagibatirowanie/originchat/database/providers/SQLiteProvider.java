package me.nagibatirowanie.originchat.database.providers;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.database.DatabaseProvider;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Провайдер для работы с SQLite
 */
public class SQLiteProvider implements DatabaseProvider {

    private final OriginChat plugin;
    private final String dbName;
    private Connection connection;
    private File databaseFile;

    public SQLiteProvider(OriginChat plugin, String dbName) {
        this.plugin = plugin;
        this.dbName = dbName;
        this.databaseFile = new File(plugin.getDataFolder(), dbName + ".db");
    }

    @Override
    public void initialize() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            plugin.getPluginLogger().info("SQLite соединение установлено.");
        } catch (ClassNotFoundException e) {
            plugin.getPluginLogger().severe("Не удалось найти драйвер SQLite: " + e.getMessage());
            throw new SQLException("Не удалось найти драйвер SQLite", e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            initialize();
        }
        return connection;
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getPluginLogger().info("SQLite соединение закрыто.");
            } catch (SQLException e) {
                plugin.getPluginLogger().warning("Ошибка при закрытии SQLite соединения: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public void migrate() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            // Создаем базовые таблицы
            statement.execute("CREATE TABLE IF NOT EXISTS oc_players (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(16) NOT NULL, " +
                    "locale VARCHAR(10) DEFAULT 'en', " +
                    "translate_enabled INTEGER DEFAULT 0, " +
                    "first_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "last_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // Создаем таблицу для настроек перевода
            statement.execute("CREATE TABLE IF NOT EXISTS oc_translate_settings (" +
                    "player_uuid VARCHAR(36) PRIMARY KEY, " +
                    "enabled INTEGER DEFAULT 0, " +
                    "source_lang VARCHAR(10) DEFAULT 'auto', " +
                    "target_lang VARCHAR(10) DEFAULT 'en', " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");");
            
            plugin.getPluginLogger().info("SQLite миграции выполнены успешно.");
        }
    }

    @Override
    public String getType() {
        return "sqlite";
    }
}