package me.nagibatirowanie.originchat;

import me.nagibatirowanie.originchat.config.ConfigManager;
import me.nagibatirowanie.originchat.locale.LocaleManager;
import me.nagibatirowanie.originchat.module.ModuleManager;
import me.nagibatirowanie.originchat.utils.LoggerUtil;
import org.bukkit.plugin.java.JavaPlugin;

public final class OriginChat extends JavaPlugin {

    private static OriginChat instance;
    private ConfigManager configManager;
    private ModuleManager moduleManager;
    private LocaleManager localeManager;
    private LoggerUtil logger;

    @Override
    public void onEnable() {
        instance = this;
        logger = new LoggerUtil(this);
        
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        
        localeManager = new LocaleManager(this);
        
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
     * Get logging utility
     * @return logging utility
     */
    public LoggerUtil getPluginLogger() {
        return logger;
    }
}
