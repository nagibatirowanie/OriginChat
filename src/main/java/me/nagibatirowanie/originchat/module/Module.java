package me.nagibatirowanie.originchat.module;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Interface for plugin modules
 */
public interface Module {

    /**
     * Get module ID
     * @return module ID
     */
    String getId();

    /**
     * Get module name
     * @return module name
     */
    String getName();

    /**
     * Get module description
     * @return module description
     */
    String getDescription();

    /**
     * Get module version
     * @return module version
     */
    String getVersion();

    /**
     * Get module configuration
     * @return module configuration or null if missing
     */
    FileConfiguration getConfig();

    /**
     * Called when the module is switched on
     */
    void onEnable();

    /**
     * Called when the module is switched off
     */
    void onDisable();

    /**
     * Reload module 
     */
    default void reload() {
        onDisable();
        onEnable();
    }
    
    /**
     * Get the name of the module configuration file
     * @return configuration file name or null if missing
     */
    default String getConfigName() {
        return null;
    }
}