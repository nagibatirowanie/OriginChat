package me.nagibatirowanie.originchat.animation;

import me.nagibatirowanie.originchat.OriginChat;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Класс для управления анимированными сообщениями в чате
 */
public class AnimatedMessage {

    private static final Map<UUID, AnimatedMessage> activeAnimations = new HashMap<>();
    private static final Object animationLock = new Object(); // Объект для синхронизации
    
    private final OriginChat plugin;
    private final Player player;
    private final Animation animation;
    private BukkitTask task;
    private int currentFrame = 0;
    
    /**
     * Создает новое анимированное сообщение для игрока
     * @param plugin экземпляр плагина
     * @param player игрок, которому будет отправляться сообщение
     * @param animation анимация для отображения
     */
    public AnimatedMessage(OriginChat plugin, Player player, Animation animation) {
        this.plugin = plugin;
        this.player = player;
        this.animation = animation;
    }
    
    /**
     * Запускает отображение анимированного сообщения
     * @param duration продолжительность показа в секундах (0 для бесконечного показа)
     */
    public void start(int duration) {
        try {
            // Остановить предыдущую анимацию для этого игрока, если она существует
            stop(player.getUniqueId());
            
            // Сохраняем эту анимацию как активную для данного игрока
            synchronized (animationLock) {
                activeAnimations.put(player.getUniqueId(), this);
            }
            
            // Запускаем задачу обновления сообщения
            task = new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        // Получаем язык игрока
                        String locale = Animation.DEFAULT_LOCALE;
                        if (player != null && player.isOnline()) {
                            // Получаем язык из LocaleManager
                            locale = plugin.getLocaleManager().getPlayerLocale(player);
                            // Проверяем, есть ли кадры для этого языка
                            if (!animation.hasLocale(locale)) {
                                locale = Animation.DEFAULT_LOCALE; // Используем язык по умолчанию, если нет перевода
                            }
                        }
                        
                        // Получаем текущий кадр для языка игрока и отправляем его
                        String frame = animation.getCurrentFrame(locale);
                        // Заменяем плейсхолдер игрока, если он есть
                        if (frame != null && frame.contains("{player}")) {
                            frame = frame.replace("{player}", player.getName());
                        }
                        if (player != null && player.isOnline()) {
                            player.sendMessage(frame);
                        } else {
                            // Если игрок оффлайн, останавливаем анимацию
                            stop(player.getUniqueId());
                        }
                    } catch (Exception e) {
                        plugin.getPluginLogger().warning("Ошибка при отображении анимации: " + e.getMessage());
                        stop(player.getUniqueId()); // Останавливаем анимацию при ошибке
                    }
                }
            }.runTaskTimer(plugin, 0, animation.getInterval());
            
            // Если указана продолжительность, останавливаем анимацию через указанное время
            if (duration > 0) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        stop(player.getUniqueId());
                    }
                }.runTaskLater(plugin, duration * 20L); // Переводим секунды в тики (20 тиков = 1 секунда)
            }
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Ошибка при запуске анимации: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Останавливает анимированное сообщение для указанного игрока
     * @param playerId UUID игрока
     */
    public static void stop(UUID playerId) {
        if (playerId == null) return;
        
        try {
            AnimatedMessage animation;
            synchronized (animationLock) {
                animation = activeAnimations.get(playerId);
                if (animation != null && animation.task != null) {
                    animation.task.cancel();
                    activeAnimations.remove(playerId);
                }
            }
        } catch (Exception e) {
            OriginChat.getInstance().getPluginLogger().warning("Ошибка при остановке анимации: " + e.getMessage());
        }
    }
    
    /**
     * Останавливает все активные анимированные сообщения
     */
    public static void stopAll() {
        try {
            synchronized (animationLock) {
                for (AnimatedMessage animation : activeAnimations.values()) {
                    if (animation != null && animation.task != null) {
                        animation.task.cancel();
                    }
                }
                activeAnimations.clear();
            }
        } catch (Exception e) {
            OriginChat.getInstance().getPluginLogger().severe("Ошибка при остановке всех анимаций: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Проверяет, есть ли активная анимация для указанного игрока
     * @param playerId UUID игрока
     * @return true, если у игрока есть активная анимация
     */
    public static boolean hasActiveAnimation(UUID playerId) {
        if (playerId == null) return false;
        
        synchronized (animationLock) {
            return activeAnimations.containsKey(playerId);
        }
    }
}