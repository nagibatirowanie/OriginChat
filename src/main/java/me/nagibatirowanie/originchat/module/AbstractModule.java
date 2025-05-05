package me.nagibatirowanie.originchat.module;

import me.nagibatirowanie.originchat.OriginChat;
import org.bukkit.configuration.file.FileConfiguration;

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
        if (localized != null && !localized.startsWith("§cMessage not found")) {
            return localized;
        }
        return name;
    }

    protected String getLocalizedDescription(String locale) {
        String key = "modules." + id + ".description";
        String localized = plugin.getLocaleManager().getMessage(key, locale);
        if (localized != null && !localized.startsWith("§cMessage not found")) {
            return localized;
        }
        return description;
    }
}