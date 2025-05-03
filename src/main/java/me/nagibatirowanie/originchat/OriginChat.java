package me.nagibatirowanie.originchat;

import me.nagibatirowanie.originchat.config.ConfigManager;
import me.nagibatirowanie.originchat.module.ModuleManager;
import me.nagibatirowanie.originchat.utils.LoggerUtil;
import org.bukkit.plugin.java.JavaPlugin;

public final class OriginChat extends JavaPlugin {

    private static OriginChat instance;
    private ConfigManager configManager;
    private ModuleManager moduleManager;
    private LoggerUtil logger;

    @Override
    public void onEnable() {
        // Инициализация плагина
        instance = this;
        logger = new LoggerUtil(this);
        
        // Загрузка конфигурации
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        
        // Инициализация и загрузка модулей
        moduleManager = new ModuleManager(this);
        moduleManager.loadModules();
        
        // Регистрация команд
        new me.nagibatirowanie.originchat.commands.CommandManager(this);
        
        logger.info("Плагин OriginChat успешно запущен!");
    }

    @Override
    public void onDisable() {
        // Выгрузка модулей
        if (moduleManager != null) {
            moduleManager.unloadModules();
        }
        
        logger.info("Плагин OriginChat выключен!");
        instance = null;
    }
    
    /**
     * Получить экземпляр плагина
     * @return экземпляр плагина
     */
    public static OriginChat getInstance() {
        return instance;
    }
    
    /**
     * Получить менеджер конфигураций
     * @return менеджер конфигураций
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * Получить менеджер модулей
     * @return менеджер модулей
     */
    public ModuleManager getModuleManager() {
        return moduleManager;
    }
    
    /**
     * Получить утилиту логирования
     * @return утилита логирования
     */
    public LoggerUtil getPluginLogger() {
        return logger;
    }
}
