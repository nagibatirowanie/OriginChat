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

import me.nagibatirowanie.originchat.OriginChat;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Abstract class for plug-in modules
 */
public abstract class AbstractModule implements Module {

    protected final OriginChat plugin;
    protected FileConfiguration config;
    protected String id;
    protected String name;
    protected String description;
    protected String version;
    protected String configName;

    public AbstractModule(OriginChat plugin, String id, String name, String description, String version) {
        this.plugin = plugin;
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return getLocalizedName("en");
    }

    @Override
    public String getDescription() {
        return getLocalizedDescription("en");
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public FileConfiguration getConfig() {
        return config;
    }
    
    @Override
    public String getConfigName() {
        return configName;
    }

    /**
     * Load module configuration
     * @param configName configuration file name (without extension)
     */
    protected void loadModuleConfig(String configName) {
        this.configName = configName;
        config = plugin.getConfigManager().loadConfig(configName);
    }

    /**
     * Save the module configuration
     * @return conservation success
     */
    protected boolean saveModuleConfig(String configName) {
        return plugin.getConfigManager().saveConfig(configName);
    }

    /**
     * Reload the module configuration
     */
    protected void reloadModuleConfig(String configName) {
        config = plugin.getConfigManager().reloadConfig(configName);
    }

    /**
     * Send an informational message to the log
     * @param message message
     */
    protected void log(String message) {
        plugin.getPluginLogger().info("[" + name + "] " + message);
    }

    /**
     * Send a debug message to the log
     * @param message message
     */
    protected void debug(String message) {
        plugin.getPluginLogger().debug("[" + name + "] " + message);
    }

    protected String getLocalizedName(String locale) {
        String key = "modules." + id + ".name";
        String localized = plugin.getLocaleManager().getMessage(key, locale);
        if (localized != null && !localized.startsWith("§cMessage not found") && !localized.equals(key)) {
            return localized;
        }
        return name;
    }

    protected String getLocalizedDescription(String locale) {
        String key = "modules." + id + ".description";
        String localized = plugin.getLocaleManager().getMessage(key, locale);
        if (localized != null && !localized.startsWith("§cMessage not found") && !localized.equals(key)) {
            return localized;
        }
        return description;
    }

    /**
     * Получить локализованное сообщение для игрока
     * @param player игрок
     * @param key ключ сообщения
     * @return локализованное сообщение
     */
    protected String getMessage(Player player, String key) {
        return plugin.getConfigManager().getLocalizedMessage(id, key, player);
    }

    /**
     * Получить список локализованных сообщений для игрока
     * @param player игрок
     * @param key ключ списка сообщений
     * @return список локализованных сообщений
     */
    protected List<String> getMessageList(Player player, String key) {
        return plugin.getConfigManager().getLocalizedMessageList(id, key, player);
    }
}