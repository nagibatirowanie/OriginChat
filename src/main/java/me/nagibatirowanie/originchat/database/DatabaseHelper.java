package me.nagibatirowanie.originchat.database;

import me.nagibatirowanie.originchat.OriginChat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Вспомогательный класс для работы с базой данных без прямого использования SQL
 * Позволяет модулям создавать свои таблицы и работать с ними через простой API
 */
public class DatabaseHelper {

    private final OriginChat plugin;
    private final String tablePrefix;
    
    /**
     * Создает новый экземпляр DatabaseHelper
     * @param plugin экземпляр плагина
     */
    public DatabaseHelper(OriginChat plugin) {
        this.plugin = plugin;
        this.tablePrefix = "oc_";
    }
    
    /**
     * Создает новый экземпляр DatabaseHelper с указанным префиксом таблиц
     * @param plugin экземпляр плагина
     * @param tablePrefix префикс для таблиц
     */
    public DatabaseHelper(OriginChat plugin, String tablePrefix) {
        this.plugin = plugin;
        this.tablePrefix = tablePrefix;
    }
    
    /**
     * Проверяет, существует ли таблица в базе данных
     * @param tableName имя таблицы (без префикса)
     * @return true, если таблица существует
     */
    public boolean tableExists(String tableName) {
        if (!plugin.getDatabaseManager().isEnabled()) {
            return false;
        }
        
        String fullTableName = tablePrefix + tableName;
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, fullTableName, new String[] {"TABLE"})) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getPluginLogger().warning("Ошибка при проверке существования таблицы: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Создает новую таблицу для модуля
     * @param tableName имя таблицы (без префикса)
     * @param primaryKey имя первичного ключа
     * @param primaryKeyType тип первичного ключа (например, "VARCHAR(36)" или "INTEGER")
     * @param autoIncrement true, если первичный ключ должен быть автоинкрементным (работает только с INTEGER)
     * @return true, если таблица успешно создана
     */
    public boolean createTable(String tableName, String primaryKey, String primaryKeyType, boolean autoIncrement) {
        if (!plugin.getDatabaseManager().isEnabled()) {
            plugin.getPluginLogger().severe("[DatabaseHelper] База данных отключена, невозможно создать таблицу");
            return false;
        }
        
        if (tableExists(tableName)) {
            plugin.getPluginLogger().info("[DatabaseHelper] Таблица " + tableName + " уже существует");
            return true;
        }
        
        String fullTableName = tablePrefix + tableName;
        String autoIncrementSql = autoIncrement ? " AUTO_INCREMENT" : "";
        
        // Проверяем тип базы данных и корректируем синтаксис
        String sql = "CREATE TABLE IF NOT EXISTS " + fullTableName + " (" +
                primaryKey + " " + primaryKeyType + 
                (primaryKeyType.toUpperCase().contains("INT") ? autoIncrementSql : "") + 
                " PRIMARY KEY" +
                ")";
        
        plugin.getPluginLogger().info("[DatabaseHelper] Попытка выполнить SQL-запрос: " + sql);
        
        Connection conn = null;
        Statement stmt = null;
        try {
            // Логируем информацию о соединении
            plugin.getPluginLogger().info("[DatabaseHelper] Получаем соединение с базой данных...");
            conn = plugin.getDatabaseManager().getConnection();
            stmt = conn.createStatement();
            stmt.executeUpdate(sql);
            plugin.getPluginLogger().info("Создана таблица " + fullTableName);
            return true;
        } catch (SQLException e) {
            plugin.getPluginLogger().severe("Ошибка при создании таблицы " + fullTableName + ": " + e.getMessage());
            plugin.getPluginLogger().severe("Текст SQL-запроса: " + sql);
            plugin.getPluginLogger().severe("Код ошибки SQL: " + e.getErrorCode());
            plugin.getPluginLogger().severe("Состояние SQL: " + e.getSQLState());
            
            try {
                if (conn != null) {
                    plugin.getPluginLogger().severe("Состояние соединения: " + (conn.isClosed() ? "закрыто" : "активно"));
                    plugin.getPluginLogger().severe("Автокоммит: " + conn.getAutoCommit());
                    plugin.getPluginLogger().severe("Уровень изоляции транзакций: " + conn.getTransactionIsolation());
                }
            } catch (SQLException connEx) {
                plugin.getPluginLogger().severe("Не удалось получить информацию о состоянии соединения: " + connEx.getMessage());
            }
            
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            plugin.getPluginLogger().severe("Стек исключения: " + sw.toString());
            return false;
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                plugin.getPluginLogger().warning("Ошибка при закрытии соединения: " + e.getMessage());
            }
        }
    }
    
    /**
     * Проверяет, существует ли колонка в таблице
     * @param tableName имя таблицы (без префикса)
     * @param columnName имя колонки
     * @return true, если колонка существует
     */
    public boolean columnExists(String tableName, String columnName) {
        if (!plugin.getDatabaseManager().isEnabled()) {
            return false;
        }
        
        String fullTableName = tablePrefix + tableName;
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, fullTableName, columnName)) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getPluginLogger().warning("Ошибка при проверке существования колонки: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Добавляет новую колонку в таблицу
     * @param tableName имя таблицы (без префикса)
     * @param columnName имя колонки
     * @param columnType тип колонки (например, "VARCHAR(255)" или "INTEGER")
     * @param defaultValue значение по умолчанию (может быть null)
     * @return true, если колонка успешно добавлена
     */
    public boolean addColumn(String tableName, String columnName, String columnType, String defaultValue) {
        if (!plugin.getDatabaseManager().isEnabled()) {
            return false;
        }
        
        if (columnExists(tableName, columnName)) {
            return true; // Колонка уже существует
        }
        
        String fullTableName = tablePrefix + tableName;
        String defaultSql = defaultValue != null ? " DEFAULT '" + defaultValue + "'" : "";
        
        String sql = "ALTER TABLE " + fullTableName + " ADD COLUMN " + columnName + " " + columnType + defaultSql;
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            plugin.getPluginLogger().info("Добавлена колонка " + columnName + " в таблицу " + fullTableName);
            return true;
        } catch (SQLException e) {
            plugin.getPluginLogger().severe("Ошибка при добавлении колонки " + columnName + " в таблицу " + fullTableName + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Вставляет или обновляет данные в таблице
     * @param tableName имя таблицы (без префикса)
     * @param primaryKeyColumn имя колонки первичного ключа
     * @param primaryKeyValue значение первичного ключа
     * @param data карта с данными для вставки/обновления (ключ - имя колонки, значение - значение)
     * @return true, если данные успешно вставлены/обновлены
     */
    public boolean saveData(String tableName, String primaryKeyColumn, Object primaryKeyValue, Map<String, Object> data) {
        if (!plugin.getDatabaseManager().isEnabled() || data.isEmpty()) {
            return false;
        }
        
        String fullTableName = tablePrefix + tableName;
        
        // Проверяем, существует ли запись с таким первичным ключом
        boolean exists = false;
        String checkSql = "SELECT 1 FROM " + fullTableName + " WHERE " + primaryKeyColumn + " = ?";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            
            if (primaryKeyValue instanceof String) {
                ps.setString(1, (String) primaryKeyValue);
            } else if (primaryKeyValue instanceof Integer) {
                ps.setInt(1, (Integer) primaryKeyValue);
            } else if (primaryKeyValue instanceof Long) {
                ps.setLong(1, (Long) primaryKeyValue);
            } else {
                ps.setObject(1, primaryKeyValue);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                exists = rs.next();
            }
            
            // Формируем SQL запрос в зависимости от того, существует ли запись
            StringBuilder sql = new StringBuilder();
            List<Object> values = new ArrayList<>();
            
            if (exists) {
                // UPDATE
                sql.append("UPDATE ").append(fullTableName).append(" SET ");
                
                int i = 0;
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    if (i > 0) {
                        sql.append(", ");
                    }
                    sql.append(entry.getKey()).append(" = ?");
                    values.add(entry.getValue());
                    i++;
                }
                
                sql.append(" WHERE ").append(primaryKeyColumn).append(" = ?");
                values.add(primaryKeyValue);
            } else {
                // INSERT
                sql.append("INSERT INTO ").append(fullTableName).append(" (");
                
                // Добавляем первичный ключ в список колонок, если его нет в data
                if (!data.containsKey(primaryKeyColumn)) {
                    sql.append(primaryKeyColumn);
                    for (String key : data.keySet()) {
                        sql.append(", ").append(key);
                    }
                } else {
                    int i = 0;
                    for (String key : data.keySet()) {
                        if (i > 0) {
                            sql.append(", ");
                        }
                        sql.append(key);
                        i++;
                    }
                }
                
                sql.append(") VALUES (");
                
                // Добавляем значение первичного ключа, если его нет в data
                if (!data.containsKey(primaryKeyColumn)) {
                    sql.append("?");
                    values.add(primaryKeyValue);
                    for (Object value : data.values()) {
                        sql.append(", ?");
                        values.add(value);
                    }
                } else {
                    for (int i = 0; i < data.size(); i++) {
                        if (i > 0) {
                            sql.append(", ");
                        }
                        sql.append("?");
                    }
                    values.addAll(data.values());
                }
                
                sql.append(")");
            }
            
            try (PreparedStatement ps2 = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < values.size(); i++) {
                    setParameter(ps2, i + 1, values.get(i));
                }
                
                ps2.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            plugin.getPluginLogger().severe("Ошибка при сохранении данных в таблицу " + fullTableName + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Получает данные из таблицы по условию
     * @param tableName имя таблицы (без префикса)
     * @param whereColumn колонка для условия WHERE (может быть null для выборки всех записей)
     * @param whereValue значение для условия WHERE
     * @param columns список колонок для выборки (пустой список = все колонки)
     * @return список карт с данными (ключ - имя колонки, значение - значение)
     */
    public List<Map<String, Object>> getData(String tableName, String whereColumn, Object whereValue, List<String> columns) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        if (!plugin.getDatabaseManager().isEnabled()) {
            return result;
        }
        
        String fullTableName = tablePrefix + tableName;
        
        StringBuilder sql = new StringBuilder("SELECT ");
        
        if (columns.isEmpty()) {
            sql.append("*");
        } else {
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append(columns.get(i));
            }
        }
        
        sql.append(" FROM ").append(fullTableName);
        
        if (whereColumn != null) {
            sql.append(" WHERE ").append(whereColumn).append(" = ?");
        }
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            
            if (whereColumn != null) {
                setParameter(ps, 1, whereValue);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    
                    if (columns.isEmpty()) {
                        // Если колонки не указаны, получаем все колонки из ResultSet
                        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                            String columnName = rs.getMetaData().getColumnName(i);
                            row.put(columnName, rs.getObject(i));
                        }
                    } else {
                        // Иначе получаем только указанные колонки
                        for (String column : columns) {
                            row.put(column, rs.getObject(column));
                        }
                    }
                    
                    result.add(row);
                }
            }
        } catch (SQLException e) {
            plugin.getPluginLogger().severe("Ошибка при получении данных из таблицы " + fullTableName + ": " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Получает одну запись из таблицы по условию
     * @param tableName имя таблицы (без префикса)
     * @param whereColumn колонка для условия WHERE
     * @param whereValue значение для условия WHERE
     * @param columns список колонок для выборки (пустой список = все колонки)
     * @return карта с данными (ключ - имя колонки, значение - значение) или null, если запись не найдена
     */
    public Map<String, Object> getDataSingle(String tableName, String whereColumn, Object whereValue, List<String> columns) {
        List<Map<String, Object>> data = getData(tableName, whereColumn, whereValue, columns);
        return data.isEmpty() ? null : data.get(0);
    }
    
    /**
     * Удаляет данные из таблицы по условию
     * @param tableName имя таблицы (без префикса)
     * @param whereColumn колонка для условия WHERE
     * @param whereValue значение для условия WHERE
     * @return true, если данные успешно удалены
     */
    public boolean deleteData(String tableName, String whereColumn, Object whereValue) {
        if (!plugin.getDatabaseManager().isEnabled()) {
            return false;
        }
        
        String fullTableName = tablePrefix + tableName;
        String sql = "DELETE FROM " + fullTableName + " WHERE " + whereColumn + " = ?";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            setParameter(ps, 1, whereValue);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getPluginLogger().severe("Ошибка при удалении данных из таблицы " + fullTableName + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Выполняет произвольный SQL запрос
     * @param sql SQL запрос
     * @param params параметры запроса
     * @return true, если запрос успешно выполнен
     */
    public boolean executeQuery(String sql, Object... params) {
        if (!plugin.getDatabaseManager().isEnabled()) {
            return false;
        }
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.length; i++) {
                setParameter(ps, i + 1, params[i]);
            }
            
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getPluginLogger().severe("Ошибка при выполнении SQL запроса: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Выполняет транзакцию с несколькими SQL запросами
     * @param callback функциональный интерфейс для выполнения запросов в транзакции
     * @return true, если транзакция успешно выполнена
     */
    public boolean executeTransaction(TransactionCallback callback) {
        if (!plugin.getDatabaseManager().isEnabled()) {
            return false;
        }
        
        Connection conn = null;
        boolean autoCommit = true;
        
        try {
            conn = plugin.getDatabaseManager().getConnection();
            autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            
            boolean result = callback.execute(conn);
            
            if (result) {
                conn.commit();
            } else {
                conn.rollback();
            }
            
            return result;
        } catch (SQLException e) {
            plugin.getPluginLogger().severe("Ошибка при выполнении транзакции: " + e.getMessage());
            
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    plugin.getPluginLogger().severe("Ошибка при откате транзакции: " + ex.getMessage());
                }
            }
            
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(autoCommit);
                    conn.close();
                } catch (SQLException e) {
                    plugin.getPluginLogger().severe("Ошибка при закрытии соединения: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Устанавливает параметр в PreparedStatement в зависимости от типа
     * @param ps PreparedStatement
     * @param index индекс параметра
     * @param value значение параметра
     * @throws SQLException при ошибке установки параметра
     */
    private void setParameter(PreparedStatement ps, int index, Object value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.NULL);
        } else if (value instanceof String) {
            ps.setString(index, (String) value);
        } else if (value instanceof Integer) {
            ps.setInt(index, (Integer) value);
        } else if (value instanceof Long) {
            ps.setLong(index, (Long) value);
        } else if (value instanceof Double) {
            ps.setDouble(index, (Double) value);
        } else if (value instanceof Boolean) {
            ps.setBoolean(index, (Boolean) value);
        } else if (value instanceof java.util.Date) {
            ps.setTimestamp(index, new java.sql.Timestamp(((java.util.Date) value).getTime()));
        } else {
            ps.setObject(index, value);
        }
    }
    
    /**
     * Функциональный интерфейс для выполнения транзакций
     */
    public interface TransactionCallback {
        /**
         * Выполняет операции в транзакции
         * @param connection соединение с базой данных
         * @return true, если транзакция должна быть подтверждена, false для отката
         * @throws SQLException при ошибке выполнения SQL запросов
         */
        boolean execute(Connection connection) throws SQLException;
    }
}