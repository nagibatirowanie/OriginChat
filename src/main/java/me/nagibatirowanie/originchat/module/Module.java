package me.nagibatirowanie.originchat.module;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Интерфейс для модулей плагина
 */
public interface Module {

    /**
     * Получить ID модуля
     * @return ID модуля
     */
    String getId();

    /**
     * Получить название модуля
     * @return название модуля
     */
    String getName();

    /**
     * Получить описание модуля
     * @return описание модуля
     */
    String getDescription();

    /**
     * Получить версию модуля
     * @return версия модуля
     */
    String getVersion();

    /**
     * Получить конфигурацию модуля
     * @return конфигурация модуля или null, если отсутствует
     */
    FileConfiguration getConfig();

    /**
     * Вызывается при включении модуля
     */
    void onEnable();

    /**
     * Вызывается при выключении модуля
     */
    void onDisable();

    /**
     * Перезагрузить модуль
     */
    default void reload() {
        onDisable();
        onEnable();
    }
    
    /**
     * Получить имя конфигурационного файла модуля
     * @return имя конфигурационного файла или null, если отсутствует
     */
    default String getConfigName() {
        return null;
    }
}