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


 package me.nagibatirowanie.originchat;

 import me.nagibatirowanie.originchat.animation.AnimationManager;
 import me.nagibatirowanie.originchat.config.ConfigManager;
 import me.nagibatirowanie.originchat.database.DatabaseManager;
 import me.nagibatirowanie.originchat.locale.LocaleManager;
 import me.nagibatirowanie.originchat.module.ModuleManager;
 import me.nagibatirowanie.originchat.translate.TranslateManager;
 import me.nagibatirowanie.originchat.utils.LoggerUtil;
 import org.bukkit.plugin.java.JavaPlugin;
 
 
 
 public final class OriginChat extends JavaPlugin {
 
     private static OriginChat instance;
     private ConfigManager configManager;
     private ModuleManager moduleManager;
     private LocaleManager localeManager;
     private TranslateManager translateManager;
     private DatabaseManager databaseManager;
     private AnimationManager animationManager;
     private LoggerUtil logger;
     
 
     @Override
     public void onEnable() {
         // Register animation listener
         new me.nagibatirowanie.originchat.animation.AnimationListener(this);
         instance = this;
         logger = new LoggerUtil(this);
         
         configManager = new ConfigManager(this);
         configManager.loadConfigs();
         
         // Initialize the database
         databaseManager = new DatabaseManager(this);
         databaseManager.initialize();
         
         // Check that the database is actually initialized
         if (!databaseManager.isEnabled()) {
             logger.warning("Database is not initialized or connection is not established. Some functions may not work correctly.");
             // Try to initialize again
             try {
                 logger.info("Attempting to reinitialize the database...");
                 databaseManager.initialize();
             } catch (Exception e) {
                 logger.severe("Error during database reinitialization: " + e.getMessage());
             }
         }
         
         // Initialize managers after database
         localeManager = new LocaleManager(this);
         
         // Initialize TranslateManager after database
         translateManager = new TranslateManager(this);
         
         // Initialize animation manager
         animationManager = new AnimationManager(this);
         
         moduleManager = new ModuleManager(this);
         moduleManager.loadModules();
     
     
         new me.nagibatirowanie.originchat.commands.CommandManager(this);
         
         logger.info("OriginChat successfully enabled :3");
 
     }
 
     @Override
     public void onDisable() {
         if (moduleManager != null) {
             moduleManager.unloadModules();
         }
         
         if (animationManager != null) {
             animationManager.stopAnimationTask();
         }
         
         if (databaseManager != null) {
             databaseManager.close();
         }
         
         logger.info("OriginChat disabled. Bye-bye!");
         instance = null;
     }
     
     /**
      * Get plugin instance
      * @return plugin instance
      */
     public static OriginChat getInstance() {
         return instance;
     }
     
     /**
      * Get configuration manager
      * @return configuration manager
      */
     public ConfigManager getConfigManager() {
         return configManager;
     }
     
     /**
      * Get module manager
      * @return module manager
      */
     
     /**
      * Get animation manager
      * @return animation manager
      */
     public AnimationManager getAnimationManager() {
         return animationManager;
     }
     
     /**
      * Get module manager
      * @return module manager
      */
     public ModuleManager getModuleManager() {
         return moduleManager;
     }
     
     /**
      * Get locale manager
      * @return locale manager
      */
     public LocaleManager getLocaleManager() {
         return localeManager;
     }
     
     /**
      * Get translate manager
      * @return translate manager
      */
     public TranslateManager getTranslateManager() {
         return translateManager;
     }
     
     /**
      * Get database manager
      * @return database manager
      */
     public DatabaseManager getDatabaseManager() {
         return databaseManager;
     }
     
     /**
      * Get logging utility
      * @return logging utility
      */
     public LoggerUtil getPluginLogger() {
         return logger;
     }
 
 }