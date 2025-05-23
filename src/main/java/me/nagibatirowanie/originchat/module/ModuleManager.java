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
import me.nagibatirowanie.originchat.module.modules.AfkModule;
import me.nagibatirowanie.originchat.module.modules.BedMessagesModule;
import me.nagibatirowanie.originchat.module.modules.ChatBubblesModule;
import me.nagibatirowanie.originchat.module.modules.ChatModule;
import me.nagibatirowanie.originchat.module.modules.EmojiModule;
import me.nagibatirowanie.originchat.module.modules.LocaleAdvancementsModule;
import me.nagibatirowanie.originchat.module.modules.LocaleDeathsModule;
import me.nagibatirowanie.originchat.module.modules.MentionsModule;
import me.nagibatirowanie.originchat.module.modules.PlayerInfoModule;
import me.nagibatirowanie.originchat.module.modules.PrivateMessageModule;
import me.nagibatirowanie.originchat.module.modules.RoleplayModule;
import me.nagibatirowanie.originchat.module.modules.ScoreboardModule;
import me.nagibatirowanie.originchat.module.modules.ServerBrandModule;
import me.nagibatirowanie.originchat.module.modules.servermessages.ServerMessagesModule;
import me.nagibatirowanie.originchat.module.modules.TabModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plugin module manager
 */
public class ModuleManager {

    private final OriginChat plugin;
    private final Map<String, Module> modules;
    private final List<String> enabledModules;

    public ModuleManager(OriginChat plugin) {
        this.plugin = plugin;
        this.modules = new HashMap<>();
        this.enabledModules = new ArrayList<>();
    }

    /**
     * Load all modules
     */
    public void loadModules() {
        // register all modules
        registerModule(new AfkModule(plugin));
        registerModule(new ChatModule(plugin));
        registerModule(new TabModule(plugin));
        registerModule(new PrivateMessageModule(plugin));
        registerModule(new RoleplayModule(plugin));
        registerModule(new ServerMessagesModule(plugin));
        registerModule(new LocaleAdvancementsModule(plugin));
        registerModule(new LocaleDeathsModule(plugin));
        registerModule(new ScoreboardModule(plugin));
        registerModule(new ServerBrandModule(plugin));
        registerModule(new MentionsModule(plugin));
        registerModule(new PlayerInfoModule(plugin));
        registerModule(new EmojiModule(plugin));
        registerModule(new ChatBubblesModule(plugin));
        registerModule(new BedMessagesModule(plugin));

        List<String> enabledFromConfig = plugin.getConfigManager().getMainConfig().getStringList("modules.enabled");
        
        for (String moduleId : enabledFromConfig) {
            enableModule(moduleId);
        }
        
    }


    public void unloadModules() {
        for (String moduleId : new ArrayList<>(enabledModules)) {
            disableModule(moduleId);
        }
        
        modules.clear();
        enabledModules.clear();
    }

    /**
     * Register module
     * @param module module to be registered
     */
    public void registerModule(Module module) {
        modules.put(module.getId(), module);
    }

    /**
     * Enavle module by ID
     * @param moduleId module ID
     * @return successful enabling
     */
    public boolean enableModule(String moduleId) {
        if (!modules.containsKey(moduleId)) {
            plugin.getPluginLogger().warning("Module '" + moduleId + "' not found!");
            return false;
        }
        
        if (enabledModules.contains(moduleId)) {
            return true; 
        }
        
        Module module = modules.get(moduleId);
        try {
            module.onEnable();
            enabledModules.add(moduleId);
            return true;
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Error when switching on the module '" + moduleId + "': " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Disable module by ID
     * @param moduleId module ID
     * @return shutdown success
     */
    public boolean disableModule(String moduleId) {
        if (!modules.containsKey(moduleId)) {
            plugin.getPluginLogger().warning("Module '" + moduleId + "' not found!");
            return false;
        }
        
        if (!enabledModules.contains(moduleId)) {
            return true;
        }
        
        Module module = modules.get(moduleId);
        try {
            module.onDisable();
            enabledModules.remove(moduleId);
            return true;
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Error when disable the module '" + moduleId + "': " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get module by ID
     * @param moduleId module ID
     * @return module or null if not found
     */
    public Module getModule(String moduleId) {
        return modules.get(moduleId);
    }

    /**
     * Check if the module is enabled
     * @param moduleId module ID
     * @return true, if the module is enabled
     */
    public boolean isModuleEnabled(String moduleId) {
        return enabledModules.contains(moduleId);
    }

    /**
     * Get a list of all registered modules
     * @return modules list
     */
    public Map<String, Module> getModules() {
        return modules;
    }

    /**
     * Get a list of enabled modules
     * @return list of enabled module IDs
     */
    public List<String> getEnabledModules() {
        return enabledModules;
    }
    
    /**
     * Reload module by ID
     * @param moduleId module ID
     * @return reload success
     */
    public boolean reloadModule(String moduleId) {
        if (!modules.containsKey(moduleId)) {
            plugin.getPluginLogger().warning("Module '" + moduleId + "' not found!");
            return false;
        }
        
        boolean wasEnabled = enabledModules.contains(moduleId);
        
        if (wasEnabled) {
            if (!disableModule(moduleId)) {
                plugin.getPluginLogger().warning("Failed to shut down the module '" + moduleId + "' to reload!");
                return false;
            }
        }
        
        Module module = modules.get(moduleId);
        if (module.getConfigName() != null && !module.getConfigName().isEmpty()) {
            plugin.getConfigManager().reloadConfig(module.getConfigName());
        }

        plugin.getLocaleManager().loadLocales();
        
        if (wasEnabled) {
            if (!enableModule(moduleId)) {
                plugin.getPluginLogger().severe("Failed to enable the module '" + moduleId + "' after the reload!");
                return false;
            }
        }
        
        plugin.getPluginLogger().info("Module '" + moduleId + "' successfully rebooted!");
        return true;
    }
}