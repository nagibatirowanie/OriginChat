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
        // Остановить предыдущую анимацию для этого игрока, если она существует
        stop(player.getUniqueId());
        
        // Сохраняем эту анимацию как активную для данного игрока
        activeAnimations.put(player.getUniqueId(), this);
        
        // Запускаем задачу обновления сообщения
        task = new BukkitRunnable() {
            @Override
            public void run() {
                // Получаем текущий кадр и отправляем его игроку
                String frame = animation.getCurrentFrame();
                // Заменяем плейсхолдер игрока, если он есть
                if (frame.contains("{player}")) {
                    frame = frame.replace("{player}", player.getName());
                }
                player.sendMessage(frame);
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
    }
    
    /**
     * Останавливает анимированное сообщение для указанного игрока
     * @param playerId UUID игрока
     */
    public static void stop(UUID playerId) {
        AnimatedMessage animation = activeAnimations.get(playerId);
        if (animation != null && animation.task != null) {
            animation.task.cancel();
            activeAnimations.remove(playerId);
        }
    }
    
    /**
     * Останавливает все активные анимированные сообщения
     */
    public static void stopAll() {
        for (AnimatedMessage animation : activeAnimations.values()) {
            if (animation.task != null) {
                animation.task.cancel();
            }
        }
        activeAnimations.clear();
    }
    
    /**
     * Проверяет, есть ли активная анимация для указанного игрока
     * @param playerId UUID игрока
     * @return true, если у игрока есть активная анимация
     */
    public static boolean hasActiveAnimation(UUID playerId) {
        return activeAnimations.containsKey(playerId);
    }
}