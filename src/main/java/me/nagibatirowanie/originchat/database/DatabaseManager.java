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
import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Plugin database manager
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
     * Initialize the database manager
     */
    /**
     * Loads database configuration from the database.yml file
     * @return database configuration or null if file not found
     */
    private FileConfiguration loadDatabaseConfig() {
        File databaseConfigFile = new File(plugin.getDataFolder(), "database.yml");
        
        if (!databaseConfigFile.exists()) {
            plugin.saveResource("database.yml", false);
            plugin.getPluginLogger().info("Created database configuration file database.yml");
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(databaseConfigFile);
        
        try {
            // Get a list of configuration exceptions
            List<String> ignoredSections = new ArrayList<>();
            
            // Update configuration using the library
            com.tchristofferson.configupdater.ConfigUpdater.update(plugin, "database.yml", databaseConfigFile, ignoredSections);
            
            // Reload configuration after update
            config = YamlConfiguration.loadConfiguration(databaseConfigFile);
            plugin.getPluginLogger().info("Database configuration has been updated");
        } catch (IOException e) {
            plugin.getPluginLogger().severe("Error updating database configuration: " + e.getMessage());
            e.printStackTrace();
        }
        
        return config;
    }
    
    public void initialize() {
        // Load database configuration from a separate file
        FileConfiguration dbConfig = loadDatabaseConfig();
        
        if (dbConfig == null) {
            plugin.getPluginLogger().severe("Failed to load database configuration");
            return;
        }
        
        // Database is always enabled
        
        String type = dbConfig.getString("type", "sqlite").toLowerCase();
        plugin.getPluginLogger().info("Database type from configuration: " + type);
        
        try {
            switch (type) {
                case "sqlite":
                    String sqliteDatabase = dbConfig.getString("sqlite.database", "originchat");
                    int sqliteTimeout = dbConfig.getInt("sqlite.options.timeout", 30);
                    boolean sqliteAutoCreate = dbConfig.getBoolean("sqlite.options.auto-create", true);
                    
                    plugin.getPluginLogger().info("Connecting to SQLite database: " + sqliteDatabase);
                    provider = new SQLiteProvider(plugin, sqliteDatabase);
                    break;
                    
                case "mysql":
                    String mysqlHost = dbConfig.getString("mysql.host", "localhost");
                    int mysqlPort = dbConfig.getInt("mysql.port", 3306);
                    String mysqlDatabase = dbConfig.getString("mysql.database", "originchat");
                    String mysqlUsername = dbConfig.getString("mysql.username", "root");
                    String mysqlPassword = dbConfig.getString("mysql.password", "password");
                    boolean mysqlUseSSL = dbConfig.getBoolean("mysql.use-ssl", false);
                    
                    // Additional connection parameters
                    int mysqlMaxPoolSize = dbConfig.getInt("mysql.options.max-pool-size", 10);
                    int mysqlConnectionTimeout = dbConfig.getInt("mysql.options.connection-timeout", 30000);
                    
                    plugin.getPluginLogger().info("Connecting to MySQL database: " + mysqlHost + ":" + mysqlPort + "/" + mysqlDatabase);
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
                    
                    plugin.getPluginLogger().info("Connecting to PostgreSQL database: " + pgHost + ":" + pgPort + "/" + pgDatabase);
                    provider = new PostgreSQLProvider(plugin, pgHost, pgPort, pgDatabase, pgUsername, pgPassword, pgUseSSL);
                    break;
                    
                default:
                    plugin.getPluginLogger().warning("Unknown database type: " + type + ". Using SQLite.");
                    provider = new SQLiteProvider(plugin, "originchat");
                    break;
            }
            // Check if migration is needed
            boolean autoMigrate = dbConfig.getBoolean("migration.auto-migrate", true);
            boolean backupBeforeMigrate = dbConfig.getBoolean("migration.backup-before-migrate", true);
            
            provider.initialize();
            
            if (autoMigrate) {
                plugin.getPluginLogger().info("Performing database migration...");
                if (backupBeforeMigrate) {
                    plugin.getPluginLogger().info("Creating database backup before migration...");
                    // Code for creating backup can be added here
                }
                provider.migrate();
            }
            
            plugin.getPluginLogger().info("Database " + provider.getType() + " successfully initialized.");
            
            // Register commands for database operations after provider initialization
            databaseCommands.registerCommands();
        } catch (SQLException e) {
            plugin.getPluginLogger().severe("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Close the database connection
     */
    public void close() {
        if (provider != null) {
            provider.close();
        }
        
        // Unregister commands
        databaseCommands.unregisterCommands();
    }

    /**
     * Get database connection
     * @return database connection
     * @throws SQLException on connection error
     */
    public Connection getConnection() throws SQLException {
        if (provider == null) {
            throw new SQLException("Database is not initialized");
        }
        return provider.getConnection();
    }

    /**
     * Check if database is enabled
     * @return true if database is enabled
     */
    public boolean isEnabled() {
        if (provider == null) {
            return false;
        }
        
        try {
            // Check connection in a more reliable way
            Connection conn = provider.getConnection();
            if (conn == null || conn.isClosed()) {
                return false;
            }
            // Check connection functionality with a simple query
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT 1");
                return true;
            }
        } catch (SQLException e) {
            plugin.getPluginLogger().warning("Error checking database connection status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get database provider type
     * @return provider type
     */
    public String getProviderType() {
        if (provider == null) {
            // If provider is not initialized, get type from configuration
            FileConfiguration dbConfig = loadDatabaseConfig();
            if (dbConfig != null) {
                return dbConfig.getString("type", "sqlite").toLowerCase();
            }
            return "none";
        }
        return provider.getType();
    }
    
    /**
     * Saves changes to database configuration
     * @param config configuration to save
     * @return true if saved successfully
     */
    public boolean saveDatabaseConfig(FileConfiguration config) {
        if (config == null) {
            return false;
        }
        
        try {
            File databaseConfigFile = new File(plugin.getDataFolder(), "database.yml");
            config.save(databaseConfigFile);
            plugin.getPluginLogger().info("Database configuration saved successfully");
            return true;
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Error saving database configuration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Gets the current database configuration
     * @return current database configuration
     */
    public FileConfiguration getDatabaseConfig() {
        return loadDatabaseConfig();
    }
    
    /**
     * Changes database type and reloads connection
     * @param type new database type (sqlite, mysql, postgresql)
     * @return true if change was successful
     */
    public boolean changeDatabaseType(String type) {
        if (type == null || type.isEmpty()) {
            return false;
        }
        
        // Check if the specified type is supported
        if (!type.equalsIgnoreCase("sqlite") && 
            !type.equalsIgnoreCase("mysql") && 
            !type.equalsIgnoreCase("postgresql")) {
            plugin.getPluginLogger().warning("Unsupported database type: " + type);
            return false;
        }
        
        // Get current configuration
        FileConfiguration config = getDatabaseConfig();
        if (config == null) {
            return false;
        }
        
        // Save new type
        config.set("type", type.toLowerCase());
        
        // Save changes
        if (!saveDatabaseConfig(config)) {
            return false;
        }
        
        // Close current connection
        close();
        
        // Reinitialize database
        initialize();
        
        return isEnabled();
    }

    /**
     * Save or update player data
     * @param uuid Player UUID
     * @param name Player name
     * @param locale Player locale
     * @param translateEnabled whether auto-translate is enabled
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
            plugin.getPluginLogger().info("Auto-translate settings table successfully created or updated (savePlayerData). SQL: " + sql);
        } catch (SQLException e) {
            plugin.getPluginLogger().severe("Failed to create or update table for auto-translate settings. Error: " + e.getMessage() + ". SQL: " + sql);
            e.printStackTrace();
        }
    }

    /**
     * Get player locale from database
     * @param uuid Player UUID
     * @return player locale or null if player not found
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
            plugin.getPluginLogger().warning("Error getting player locale: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Check if auto-translate is enabled for player
     * @param uuid Player UUID
     * @return true if auto-translate is enabled
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
            plugin.getPluginLogger().warning("Error getting auto-translate status: " + e.getMessage());
        }
        
        return false;
    }

    /**
     * Set auto-translate status for player
     * @param uuid Player UUID
     * @param enabled auto-translate status
     */
    public void setTranslateEnabled(UUID uuid, boolean enabled) {
        if (!isEnabled()) return;
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE oc_players SET translate_enabled = ? WHERE uuid = ?")) {
            
            ps.setInt(1, enabled ? 1 : 0);
            ps.setString(2, uuid.toString());
            
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getPluginLogger().warning("Error setting auto-translate status: " + e.getMessage());
        }
    }
    
    /**
     * Save player data to database
     * @param player player
     */
    public void savePlayerData(Player player) {
        databaseCommands.savePlayerData(player);
    }
    
    /**
     * Get object for working with database commands
     * @return DatabaseCommands object
     */
    public DatabaseCommands getDatabaseCommands() {
        return databaseCommands;
    }
}