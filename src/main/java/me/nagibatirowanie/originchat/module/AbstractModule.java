package me.nagibatirowanie.originchat.module;

import me.nagibatirowanie.originchat.OriginChat;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Абстрактный класс для модулей плагина
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
        return name;
    }

    @Override
    public String getDescription() {
        return description;
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
     * Загрузить конфигурацию модуля
     * @param configName имя конфигурационного файла (без расширения)
     */
    protected void loadModuleConfig(String configName) {
        this.configName = configName;
        config = plugin.getConfigManager().loadConfig(configName);
    }

    /**
     * Сохранить конфигурацию модуля
     * @return успешность сохранения
     */
    protected boolean saveModuleConfig(String configName) {
        return plugin.getConfigManager().saveConfig(configName);
    }

    /**
     * Перезагрузить конфигурацию модуля
     */
    protected void reloadModuleConfig(String configName) {
        config = plugin.getConfigManager().reloadConfig(configName);
    }

    /**
     * Отправить информационное сообщение в лог
     * @param message сообщение
     */
    protected void log(String message) {
        plugin.getPluginLogger().info("[" + name + "] " + message);
    }

    /**
     * Отправить отладочное сообщение в лог
     * @param message сообщение
     */
    protected void debug(String message) {
        plugin.getPluginLogger().debug("[" + name + "] " + message);
    }
}