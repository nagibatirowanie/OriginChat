package me.nagibatirowanie.originchat.database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Интерфейс для провайдеров баз данных
 */
public interface DatabaseProvider {
    
    /**
     * Инициализировать соединение с базой данных
     * @throws SQLException при ошибке соединения
     */
    void initialize() throws SQLException;
    
    /**
     * Получить соединение с базой данных
     * @return соединение с базой данных
     * @throws SQLException при ошибке соединения
     */
    Connection getConnection() throws SQLException;
    
    /**
     * Закрыть соединение с базой данных
     */
    void close();
    
    /**
     * Проверить соединение с базой данных
     * @return true, если соединение активно
     */
    boolean isConnected();
    
    /**
     * Выполнить миграции базы данных
     * @throws SQLException при ошибке выполнения миграций
     */
    void migrate() throws SQLException;
    
    /**
     * Получить тип провайдера
     * @return тип провайдера
     */
    String getType();
}