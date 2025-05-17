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