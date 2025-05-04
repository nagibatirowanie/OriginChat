package me.nagibatirowanie.originchat.module;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.module.modules.ChatModule;
import me.nagibatirowanie.originchat.module.modules.PrivateMessageModule;
import me.nagibatirowanie.originchat.module.modules.RoleplayModule;
import me.nagibatirowanie.originchat.module.modules.ServerMessagesModule;
import me.nagibatirowanie.originchat.module.modules.TabModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Менеджер модулей плагина
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
     * Загрузить все модули
     */
    public void loadModules() {
        // Регистрация всех модулей
        registerModule(new ChatModule(plugin));
        registerModule(new TabModule(plugin));
        registerModule(new PrivateMessageModule(plugin));
        registerModule(new RoleplayModule(plugin));
        registerModule(new ServerMessagesModule(plugin));

        // Получение списка включенных модулей из конфига
        List<String> enabledFromConfig = plugin.getConfigManager().getMainConfig().getStringList("modules.enabled");
        
        // Включение модулей
        for (String moduleId : enabledFromConfig) {
            enableModule(moduleId);
        }
        
        plugin.getPluginLogger().info("Загружено модулей: " + enabledModules.size());
    }

    /**
     * Выгрузить все модули
     */
    public void unloadModules() {
        // Выключение всех активных модулей
        for (String moduleId : new ArrayList<>(enabledModules)) {
            disableModule(moduleId);
        }
        
        modules.clear();
        enabledModules.clear();
    }

    /**
     * Зарегистрировать модуль
     * @param module модуль для регистрации
     */
    public void registerModule(Module module) {
        modules.put(module.getId(), module);
    }

    /**
     * Включить модуль по ID
     * @param moduleId ID модуля
     * @return успешность включения
     */
    public boolean enableModule(String moduleId) {
        if (!modules.containsKey(moduleId)) {
            plugin.getPluginLogger().warning("Модуль '" + moduleId + "' не найден!");
            return false;
        }
        
        if (enabledModules.contains(moduleId)) {
            return true; // Модуль уже включен
        }
        
        Module module = modules.get(moduleId);
        try {
            module.onEnable();
            enabledModules.add(moduleId);
            plugin.getPluginLogger().info("Модуль '" + moduleId + "' успешно включен!");
            return true;
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Ошибка при включении модуля '" + moduleId + "': " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Выключить модуль по ID
     * @param moduleId ID модуля
     * @return успешность выключения
     */
    public boolean disableModule(String moduleId) {
        if (!modules.containsKey(moduleId)) {
            plugin.getPluginLogger().warning("Модуль '" + moduleId + "' не найден!");
            return false;
        }
        
        if (!enabledModules.contains(moduleId)) {
            return true; // Модуль уже выключен
        }
        
        Module module = modules.get(moduleId);
        try {
            module.onDisable();
            enabledModules.remove(moduleId);
            plugin.getPluginLogger().info("Модуль '" + moduleId + "' успешно выключен!");
            return true;
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Ошибка при выключении модуля '" + moduleId + "': " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Получить модуль по ID
     * @param moduleId ID модуля
     * @return модуль или null, если не найден
     */
    public Module getModule(String moduleId) {
        return modules.get(moduleId);
    }

    /**
     * Проверить, включен ли модуль
     * @param moduleId ID модуля
     * @return true, если модуль включен
     */
    public boolean isModuleEnabled(String moduleId) {
        return enabledModules.contains(moduleId);
    }

    /**
     * Получить список всех зарегистрированных модулей
     * @return список модулей
     */
    public Map<String, Module> getModules() {
        return modules;
    }

    /**
     * Получить список включенных модулей
     * @return список ID включенных модулей
     */
    public List<String> getEnabledModules() {
        return enabledModules;
    }
    
    /**
     * Перезагрузить модуль по ID
     * @param moduleId ID модуля
     * @return успешность перезагрузки
     */
    public boolean reloadModule(String moduleId) {
        if (!modules.containsKey(moduleId)) {
            plugin.getPluginLogger().warning("Модуль '" + moduleId + "' не найден!");
            return false;
        }
        
        boolean wasEnabled = enabledModules.contains(moduleId);
        
        // Если модуль был включен, выключаем его
        if (wasEnabled) {
            if (!disableModule(moduleId)) {
                plugin.getPluginLogger().warning("Не удалось выключить модуль '" + moduleId + "' для перезагрузки!");
                return false;
            }
        }
        
        // Перезагружаем конфигурацию модуля, если она существует
        Module module = modules.get(moduleId);
        if (module.getConfigName() != null && !module.getConfigName().isEmpty()) {
            plugin.getConfigManager().reloadConfig(module.getConfigName());
        }
        
        // Если модуль был включен, включаем его снова
        if (wasEnabled) {
            if (!enableModule(moduleId)) {
                plugin.getPluginLogger().severe("Не удалось включить модуль '" + moduleId + "' после перезагрузки!");
                return false;
            }
        }
        
        plugin.getPluginLogger().info("Модуль '" + moduleId + "' успешно перезагружен!");
        return true;
    }
}