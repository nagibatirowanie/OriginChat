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
        // Регистрируем слушатель анимаций
        new me.nagibatirowanie.originchat.animation.AnimationListener(this);
        instance = this;
        logger = new LoggerUtil(this);
        
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        
        // Инициализируем базу данных
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();
        
        // Проверяем, что база данных действительно инициализирована
        if (!databaseManager.isEnabled()) {
            logger.warning("База данных не инициализирована или соединение не установлено. Некоторые функции могут работать некорректно.");
            // Пробуем повторно инициализировать
            try {
                logger.info("Повторная попытка инициализации базы данных...");
                databaseManager.initialize();
            } catch (Exception e) {
                logger.severe("Ошибка при повторной инициализации базы данных: " + e.getMessage());
            }
        }
        
        // Инициализируем менеджеры после базы данных
        localeManager = new LocaleManager(this);
        
        // Инициализируем TranslateManager после базы данных
        translateManager = new TranslateManager(this);
        
        // Инициализируем менеджер анимаций
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
