package me.nagibatirowanie.originchat.animation;

import java.util.List;

/**
 * Класс, представляющий анимацию с набором кадров
 */
public class Animation {

    private final String name;
    private final int interval; // Интервал в тиках
    private final List<String> frames;
    private int currentFrameIndex;
    private int tickCounter;
    
    /**
     * Создает новую анимацию
     * @param name имя анимации
     * @param interval интервал смены кадров в тиках
     * @param frames список кадров
     */
    public Animation(String name, int interval, List<String> frames) {
        this.name = name;
        this.interval = Math.max(1, interval); // Минимальный интервал - 1 тик
        this.frames = frames;
        this.currentFrameIndex = 0;
        this.tickCounter = 0;
    }
    
    /**
     * Обновляет счетчик тиков и переключает кадр при необходимости
     */
    public void nextFrame() {
        tickCounter++;
        
        if (tickCounter >= interval) {
            tickCounter = 0;
            currentFrameIndex = (currentFrameIndex + 1) % frames.size();
        }
    }
    
    /**
     * Получает текущий кадр анимации
     * @return текущий кадр
     */
    public String getCurrentFrame() {
        if (frames.isEmpty()) {
            return "";
        }
        return frames.get(currentFrameIndex);
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
     * Получает список всех кадров
     * @return список кадров
     */
    public List<String> getFrames() {
        return frames;
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