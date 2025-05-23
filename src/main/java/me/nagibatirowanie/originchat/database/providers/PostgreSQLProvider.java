package me.nagibatirowanie.originchat.database.providers;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.database.DatabaseProvider;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Provider for working with PostgreSQL
 */
public class PostgreSQLProvider implements DatabaseProvider {

    private final OriginChat plugin;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean useSSL;
    private Connection connection;

    public PostgreSQLProvider(OriginChat plugin, String host, int port, String database, String username, String password, boolean useSSL) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.useSSL = useSSL;
    }

    @Override
    public void initialize() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");

            String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
            if (!useSSL) {
                url += "?ssl=false";
            }
            connection = DriverManager.getConnection(url, username, password);
            plugin.getPluginLogger().info("PostgreSQL connection established.");
        } catch (ClassNotFoundException e) {
            plugin.getPluginLogger().severe("PostgreSQL driver not found: " + e.getMessage());
            throw new SQLException("PostgreSQL driver not found", e);
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
                plugin.getPluginLogger().info("PostgreSQL connection closed.");
            } catch (SQLException e) {
                plugin.getPluginLogger().warning("Error while closing PostgreSQL connection: " + e.getMessage());
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
            // Create basic player data table
            statement.execute("CREATE TABLE IF NOT EXISTS oc_players (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(16) NOT NULL, " +
                    "locale VARCHAR(10) DEFAULT 'en', " +
                    "translate_enabled INTEGER DEFAULT 0, " +
                    "first_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "last_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            plugin.getPluginLogger().info("PostgreSQL migrations completed successfully.");
        }
    }

    @Override
    public String getType() {
        return "postgresql";
    }
}
