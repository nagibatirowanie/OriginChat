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

package me.nagibatirowanie.originchat.translate;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.database.DatabaseHelper;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Player auto-translation manager
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
     * Initialises a table in the database to store the auto-translation settings
     */
    private void initDatabase() {
        if (plugin.getDatabaseManager() == null) {
            plugin.getPluginLogger().warning("[TranslateManager] DatabaseManager is not initialised, the autotranslation settings will only be stored in memory");
            return;
        }
        
        // Check the database connection status
        try {
            // Try to get a connection to check if it works.
            if (plugin.getDatabaseManager().getConnection() == null) {
                plugin.getPluginLogger().warning("[TranslateManager] Failed to obtain a database connection, the auto-translation settings will only be stored in memory");
                return;
            }
        } catch (Exception e) {
            plugin.getPluginLogger().warning("[TranslateManager] Error when checking the connection to the database: " + e.getMessage());
            return;
        }
        
        // If the connection is active, but isEnabled() returns false, print a warning message
        if (!plugin.getDatabaseManager().isEnabled()) {
            plugin.getPluginLogger().warning("[TranslateManager] DatabaseManager.isEnabled() returned false, but the connection is active. Continue initialisation.");
        }

        // Create a table for auto-translation settings if it does not exist
        if (!dbHelper.tableExists(TRANSLATE_TABLE)) {
            try {
                plugin.getPluginLogger().info("[TranslateManager] Attempting to create a table " + TRANSLATE_TABLE + " on the field player_uuid VARCHAR(36)");
                boolean created = dbHelper.createTable(TRANSLATE_TABLE, "player_uuid", "VARCHAR(36)", false);
                if (created) {
                    //plugin.getPluginLogger().info("[TranslateManager] Table created successfully, add enabled column");
                    boolean columnAdded = dbHelper.addColumn(TRANSLATE_TABLE, "enabled", "INTEGER", "0");
                    if (columnAdded) {
                        //plugin.getPluginLogger().info("[TranslateManager] Table for auto-translation settings successfully created and initialised");
                    } else {
                        plugin.getPluginLogger().severe("[TranslateManager] Error when adding enabled column to the table " + TRANSLATE_TABLE);
                    }
                } else {
                    plugin.getPluginLogger().severe("[TranslateManager] Failed to create table " + TRANSLATE_TABLE + ". Check the access rights and status of the database.");
                }
            } catch (Exception e) {
                plugin.getPluginLogger().severe("[TranslateManager] Critical error when creating a table " + TRANSLATE_TABLE + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Loads auto-translation settings from the database
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
                plugin.getPluginLogger().warning("Error loading auto-translation settings from the database: " + e.getMessage());
            }
        }

        plugin.getPluginLogger().debug("Loaded auto-translation settings for " + playerTranslateSettings.size() + " players");
    }

    /**
     * Saves the auto-translation setting for the player to the database
     * @param playerUuid player UUID
     * @param enabled setting state
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
     * Checks if auto-translation is enabled for a player
     * @param player player
     * @return true if auto-translate is enabled, false if disabled
     */
    public boolean isTranslateEnabled(Player player) {
        if (player == null) {
            return false;
        }
        boolean enabled = playerTranslateSettings.getOrDefault(player.getUniqueId(), false);
        return enabled;
    }

    /**
     * Enables or disables auto-translation for a player
     * @param player player
     * @param enabled true to enable, false to disable
     * @return new setting state
     */
    public boolean setTranslateEnabled(Player player, boolean enabled) {
        if (player == null) {
            plugin.getPluginLogger().debug("[TranslateManager] setTranslateEnabled: player is null, returning false");
            return false;
        }
        
        playerTranslateSettings.put(player.getUniqueId(), enabled);
        saveToDatabase(player.getUniqueId(), enabled);
        return enabled;
    }

    /**
     * Switches the auto-translation state for the player
     * @param player player
     * @return new setting state
     */
    public boolean toggleTranslate(Player player) {
        boolean currentState = isTranslateEnabled(player);
        boolean newState = !currentState;
        setTranslateEnabled(player, newState);
        return newState;
    }

    /**
     * Example of using translation with advanced error logging
     * @param player player
     * @param text text to translate
     * @param toLang target language
     * @return translated text or null on error
     */
    public String tryTranslateWithLogging(Player player, String text, String toLang) {
        try {
            // Здесь предполагается вызов TranslateUtil.translate или translateAsync
            String translated = me.nagibatirowanie.originchat.utils.TranslateUtil.translate(text, toLang);
            return translated;
        } catch (Exception e) {
            plugin.getPluginLogger().severe(String.format("[TranslateManager] Translation error for player %s (%s) language %s. Text: '%s'. Reason: %s", player != null ? player.getName() : "null", player != null ? player.getUniqueId() : "null", toLang, text, e.getMessage()));
            e.printStackTrace();
            return null;
        }
    }
}