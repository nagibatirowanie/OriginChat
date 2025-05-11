package me.nagibatirowanie.originchat.animation;

import me.nagibatirowanie.originchat.OriginChat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Класс, представляющий анимацию с набором кадров
 * Поддерживает локализацию кадров для разных языков
 */
public class Animation {

    private final String name;
    private final int interval; // Интервал в тиках
    private final List<String> frames; // Стандартные кадры (для обратной совместимости)
    private final Map<String, List<String>> localizedFrames; // Кадры для разных языков
    private int currentFrameIndex;
    private int tickCounter;
    
    // Константа для языка по умолчанию
    public static final String DEFAULT_LOCALE = getDefaultLocale();
    
    /**
     * Создает новую анимацию
     * @param name имя анимации
     * @param interval интервал смены кадров в тиках
     * @param frames список кадров (для обратной совместимости)
     */
    public Animation(String name, int interval, List<String> frames) {
        this.name = name;
        this.interval = Math.max(1, interval); // Минимальный интервал - 1 тик
        this.frames = frames;
        this.localizedFrames = new HashMap<>();
        this.localizedFrames.put(DEFAULT_LOCALE, frames); // Добавляем стандартные кадры как локализацию по умолчанию
        this.currentFrameIndex = 0;
        this.tickCounter = 0;
    }
    
    /**
     * Создает новую анимацию с поддержкой локализации
     * @param name имя анимации
     * @param interval интервал смены кадров в тиках
     * @param localizedFrames карта локализованных кадров (ключ - код языка, значение - список кадров)
     */
    public Animation(String name, int interval, Map<String, List<String>> localizedFrames) {
        this.name = name;
        this.interval = Math.max(1, interval); // Минимальный интервал - 1 тик
        this.localizedFrames = localizedFrames;
        
        // Для обратной совместимости устанавливаем frames как кадры для языка по умолчанию
        this.frames = localizedFrames.getOrDefault(DEFAULT_LOCALE, List.of());
        
        this.currentFrameIndex = 0;
        this.tickCounter = 0;
    }
    
    /**
     * Обновляет счетчик тиков и переключает кадр при необходимости
     */
    public void nextFrame() {
        // Увеличиваем счетчик тиков и проверяем на переполнение
        tickCounter = (tickCounter + 1) % Integer.MAX_VALUE;
        
        if (tickCounter >= interval) {
            tickCounter = 0;
            // Проверяем, что список кадров не пустой
            if (!frames.isEmpty()) {
                currentFrameIndex = (currentFrameIndex + 1) % frames.size();
            }
        }
    }
    
    /**
     * Получает текущий кадр анимации для указанного языка
     * @param locale код языка (например, "ru", "en")
     * @return текущий кадр для указанного языка или для языка по умолчанию, если перевод не найден
     */
    public String getCurrentFrame(String locale) {
        // Проверяем, включено ли автоопределение языка
        boolean autoDetect = OriginChat.getInstance().getConfig().getBoolean("locale.auto_detect", true);
        
        // Если автоопределение выключено, используем язык по умолчанию
        if (!autoDetect) {
            locale = getDefaultLocale();
        }
        
        List<String> framesForLocale = getFramesForLocale(locale);
        
        if (framesForLocale == null || framesForLocale.isEmpty()) {
            return "";
        }
        
        // Дополнительная проверка на случай, если индекс выходит за пределы
        if (currentFrameIndex < 0 || currentFrameIndex >= framesForLocale.size()) {
            currentFrameIndex = 0;
            return framesForLocale.get(0);
        }
        
        return framesForLocale.get(currentFrameIndex);
    }
    
    /**
     * Получает текущий кадр анимации для языка по умолчанию
     * @return текущий кадр
     */
    public String getCurrentFrame() {
        return getCurrentFrame(DEFAULT_LOCALE);
    }
    
    /**
     * Получает имя анимации
     * @return имя анимации
     */
    public String getName() {
        return name;
    }
    
    /**
     * Получает интервал смены кадров
     * @return интервал в тиках
     */
    public int getInterval() {
        return interval;
    }
    
    /**
     * Получает список всех кадров для указанного языка
     * @param locale код языка
     * @return список кадров для указанного языка или для языка по умолчанию, если перевод не найден
     */
    public List<String> getFramesForLocale(String locale) {
        // Проверяем, включено ли автоопределение языка
        boolean autoDetect = OriginChat.getInstance().getConfig().getBoolean("locale.auto_detect", true);
        
        // Если автоопределение выключено, используем язык по умолчанию
        if (!autoDetect) {
            locale = getDefaultLocale();
        }
        
        // Если запрошенный язык отсутствует, возвращаем кадры для языка по умолчанию
        return localizedFrames.getOrDefault(locale, localizedFrames.getOrDefault(getDefaultLocale(), List.of()));
    }
    
    /**
     * Получает список всех кадров для языка по умолчанию
     * @return список кадров
     */
    public List<String> getFrames() {
        return getFramesForLocale(DEFAULT_LOCALE);
    }
    
    /**
     * Получает карту всех локализованных кадров
     * @return карта локализованных кадров
     */
    public Map<String, List<String>> getLocalizedFrames() {
        return localizedFrames;
    }
    
    /**
     * Проверяет, есть ли кадры для указанного языка
     * @param locale код языка
     * @return true, если есть кадры для указанного языка
     */
    public boolean hasLocale(String locale) {
        return localizedFrames.containsKey(locale) && !localizedFrames.get(locale).isEmpty();
    }
    
    /**
     * Получает язык по умолчанию из конфигурации плагина
     * @return код языка по умолчанию
     */
    public static String getDefaultLocale() {
        try {
            return OriginChat.getInstance().getConfig().getString("locale.default", "ru");
        } catch (Exception e) {
            return "ru"; // Возвращаем "ru" в случае ошибки
        }
    }
    
    /**
     * Получает список доступных языков для этой анимации
     * @return список кодов языков
     */
    public List<String> getAvailableLocales() {
        return List.copyOf(localizedFrames.keySet());
    }
    
    /**
     * Получает индекс текущего кадра
     * @return индекс текущего кадра
     */
    public int getCurrentFrameIndex() {
        return currentFrameIndex;
    }
    
    /**
     * Устанавливает конкретный кадр как текущий
     * @param index индекс кадра
     */
    public void setCurrentFrameIndex(int index) {
        if (frames.isEmpty()) return;
        this.currentFrameIndex = Math.max(0, Math.min(index, frames.size() - 1));
        this.tickCounter = 0;
    }
}