package me.nagibatirowanie.originchat.animation;

import me.nagibatirowanie.originchat.OriginChat;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Менеджер анимаций для текстовых сообщений
 * Поддерживает плейсхолдеры вида {animation_имяанимации}
 */
public class AnimationManager {

    private final OriginChat plugin;
    private final Map<String, Animation> animations;
    private final Pattern animationPattern;
    private FileConfiguration animationsConfig;
    private BukkitTask animationTask;
    
    public AnimationManager(OriginChat plugin) {
        this.plugin = plugin;
        this.animations = new HashMap<>();
        this.animationPattern = Pattern.compile("\\{animation_([^}]+)\\}");
        
        loadAnimationsConfig();
        loadAnimations();
        startAnimationTask();
    }
    
    /**
     * Загружает конфигурацию анимаций из файла animations.yml
     */
    private void loadAnimationsConfig() {
        File configFile = new File(plugin.getDataFolder(), "animations.yml");
        
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource("animations.yml", false);
        }
        
        animationsConfig = YamlConfiguration.loadConfiguration(configFile);
        
        // Проверяем и обновляем конфигурацию, если необходимо
        boolean updated = plugin.getConfigManager().getConfigUpdater().checkAndUpdateConfig(
                configFile, animationsConfig, "animations.yml");
        
        if (updated) {
            animationsConfig = YamlConfiguration.loadConfiguration(configFile);
            plugin.getPluginLogger().info("Конфигурация анимаций была обновлена");
        }
    }
    
    /**
     * Загружает анимации из конфигурации
     */
    public void loadAnimations() {
        animations.clear();
        
        if (animationsConfig == null) {
            plugin.getPluginLogger().warning("Не удалось загрузить анимации: конфигурация не инициализирована");
            return;
        }
        
        for (String key : animationsConfig.getKeys(false)) {
            ConfigurationSection section = animationsConfig.getConfigurationSection(key);
            if (section == null) continue;
            
            int interval = section.getInt("interval", 20); // Интервал в тиках (по умолчанию 1 секунда)
            List<String> frames = section.getStringList("frames");
            
            if (frames.isEmpty()) {
                plugin.getPluginLogger().warning("Анимация '" + key + "' не содержит кадров и будет пропущена");
                continue;
            }
            
            Animation animation = new Animation(key, interval, frames);
            animations.put(key, animation);
            plugin.getPluginLogger().info("Загружена анимация '" + key + "' с " + frames.size() + " кадрами и интервалом " + interval + " тиков");
        }
        
        plugin.getPluginLogger().info("Загружено " + animations.size() + " анимаций");
    }
    
    /**
     * Запускает задачу обновления кадров анимаций
     */
    private void startAnimationTask() {
        // Останавливаем предыдущую задачу, если она существует
        if (animationTask != null) {
            animationTask.cancel();
        }
        
        // Запускаем новую задачу, которая будет обновлять кадры анимаций
        animationTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Animation animation : animations.values()) {
                    animation.nextFrame();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L); // Запускаем каждый тик для точного контроля интервалов
    }
    
    /**
     * Останавливает задачу обновления анимаций
     */
    public void stopAnimationTask() {
        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
        }
        
        // Останавливаем все активные анимированные сообщения
        me.nagibatirowanie.originchat.animation.AnimatedMessage.stopAll();
    }
    
    /**
     * Останавливает анимированные сообщения для игрока
     * @param player игрок
     */
    public void stopPlayerAnimations(org.bukkit.entity.Player player) {
        if (player != null) {
            me.nagibatirowanie.originchat.animation.AnimatedMessage.stop(player.getUniqueId());
        }
    }
    
    /**
     * Перезагружает анимации из конфигурации
     */
    public void reloadAnimations() {
        loadAnimationsConfig();
        loadAnimations();
        startAnimationTask();
    }
    
    /**
     * Заменяет плейсхолдеры анимаций в тексте на текущие кадры
     * @param text исходный текст с плейсхолдерами
     * @param player игрок (может быть null)
     * @return текст с замененными плейсхолдерами
     */
    public String processAnimations(String text, Player player) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        Matcher matcher = animationPattern.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String animationName = matcher.group(1);
            Animation animation = animations.get(animationName);
            
            String replacement = "";
            if (animation != null) {
                replacement = animation.getCurrentFrame();
                // Обрабатываем вложенные плейсхолдеры в кадре анимации
                if (player != null && replacement.contains("{player}")) {
                    replacement = replacement.replace("{player}", player.getName());
                }
            } else {
                replacement = "[Анимация '" + animationName + "' не найдена]";
            }
            
            // Экранируем специальные символы в replacement для Matcher.appendReplacement
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * Получает список всех загруженных анимаций
     * @return список имен анимаций
     */
    public List<String> getAnimationNames() {
        return new ArrayList<>(animations.keySet());
    }
    
    /**
     * Получает анимацию по имени
     * @param name имя анимации
     * @return объект анимации или null, если анимация не найдена
     */
    public Animation getAnimation(String name) {
        return animations.get(name);
    }
}