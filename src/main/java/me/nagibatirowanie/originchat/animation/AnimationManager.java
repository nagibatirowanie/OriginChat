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
    private final Map<String, Animation> animations = new HashMap<>();
    private final Pattern animationPattern = Pattern.compile("\\{animation_([^}]+)\\}");
    private FileConfiguration animationsConfig;
    private BukkitTask animationTask;
    private final Object animationLock = new Object(); // Объект для синхронизации
    
    public AnimationManager(OriginChat plugin) {
        this.plugin = plugin;
        
        try {
            loadAnimationsConfig();
            loadAnimations();
            startAnimationTask();
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Ошибка при инициализации AnimationManager: " + e.getMessage());
            e.printStackTrace();
        }
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
        synchronized (animationLock) {
            animations.clear();
            
            if (animationsConfig == null) {
                plugin.getPluginLogger().warning("Не удалось загрузить анимации: конфигурация не инициализирована");
                return;
            }
            
            try {
                for (String key : animationsConfig.getKeys(false)) {
                    ConfigurationSection section = animationsConfig.getConfigurationSection(key);
                    if (section == null) continue;
                    
                    int interval = section.getInt("interval", 20); // Интервал в тиках (по умолчанию 1 секунда)
                    
                    // Проверяем, есть ли локализованные кадры
                    ConfigurationSection framesSection = section.getConfigurationSection("frames");
                    Animation animation;
                    
                    if (framesSection != null) {
                        // Новый формат с локализацией
                        Map<String, List<String>> localizedFrames = new HashMap<>();
                        boolean hasFrames = false;
                        
                        // Загружаем кадры для каждого языка
                        for (String locale : framesSection.getKeys(false)) {
                            List<String> localeFrames = framesSection.getStringList(locale);
                            if (!localeFrames.isEmpty()) {
                                localizedFrames.put(locale, localeFrames);
                                hasFrames = true;
                            }
                        }
                        
                        if (!hasFrames) {
                            plugin.getPluginLogger().warning("Анимация '" + key + "' не содержит кадров и будет пропущена");
                            continue;
                        }
                        
                        animation = new Animation(key, interval, localizedFrames);
                        plugin.getPluginLogger().info("Загружена анимация '" + key + "' с локализацией для " + 
                                localizedFrames.size() + " языков и интервалом " + interval + " тиков");
                    } else {
                        // Старый формат без локализации
                        List<String> frames = section.getStringList("frames");
                        
                        if (frames.isEmpty()) {
                            plugin.getPluginLogger().warning("Анимация '" + key + "' не содержит кадров и будет пропущена");
                            continue;
                        }
                        
                        animation = new Animation(key, interval, frames);
                        plugin.getPluginLogger().info("Загружена анимация '" + key + "' с " + frames.size() + 
                                " кадрами и интервалом " + interval + " тиков");
                    }
                    
                    animations.put(key, animation);
                }
                
                plugin.getPluginLogger().info("Загружено " + animations.size() + " анимаций");
            } catch (Exception e) {
                plugin.getPluginLogger().severe("Ошибка при загрузке анимаций: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Запускает задачу обновления кадров анимаций
     */
    private void startAnimationTask() {
        try {
            // Останавливаем предыдущую задачу, если она существует
            if (animationTask != null && !animationTask.isCancelled()) {
                animationTask.cancel();
            }
            
            // Запускаем новую задачу, которая будет обновлять кадры анимаций
            animationTask = new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        synchronized (animationLock) {
                            for (Animation animation : animations.values()) {
                                if (animation != null) {
                                    animation.nextFrame();
                                }
                            }
                        }
                    } catch (Exception e) {
                        plugin.getPluginLogger().severe("Ошибка при обновлении кадров анимации: " + e.getMessage());
                    }
                }
            }.runTaskTimer(plugin, 1L, 1L); // Запускаем каждый тик для точного контроля интервалов
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Ошибка при запуске задачи анимации: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Останавливает задачу обновления анимаций
     */
    public void stopAnimationTask() {
        try {
            if (animationTask != null && !animationTask.isCancelled()) {
                animationTask.cancel();
                animationTask = null;
            }
            
            // Останавливаем все активные анимированные сообщения
            me.nagibatirowanie.originchat.animation.AnimatedMessage.stopAll();
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Ошибка при остановке задачи анимации: " + e.getMessage());
            e.printStackTrace();
        }
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
        
        try {
            Matcher matcher = animationPattern.matcher(text);
            StringBuffer result = new StringBuffer();
            
            while (matcher.find()) {
                String animationName = matcher.group(1);
                Animation animation;
                
                synchronized (animationLock) {
                    animation = animations.get(animationName);
                }
                
                String replacement = "";
                if (animation != null) {
                    // Получаем язык игрока, если игрок онлайн
                    String locale = Animation.DEFAULT_LOCALE;
                    if (player != null) {
                        // Получаем язык из LocaleManager
                        locale = plugin.getLocaleManager().getPlayerLocale(player);
                        // Проверяем, есть ли кадры для этого языка
                        if (!animation.hasLocale(locale)) {
                            locale = Animation.DEFAULT_LOCALE; // Используем язык по умолчанию, если нет перевода
                        }
                    }
                    
                    replacement = animation.getCurrentFrame(locale);
                    // Обрабатываем вложенные плейсхолдеры в кадре анимации
                    if (player != null && replacement != null && replacement.contains("{player}")) {
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
        } catch (Exception e) {
            plugin.getPluginLogger().warning("Ошибка при обработке анимаций в тексте: " + e.getMessage());
            return text; // Возвращаем исходный текст в случае ошибки
        }
    }
    
    /**
     * Получает список всех загруженных анимаций
     * @return список имен анимаций
     */
    public List<String> getAnimationNames() {
        synchronized (animationLock) {
            return new ArrayList<>(animations.keySet());
        }
    }
    
    /**
     * Получает анимацию по имени
     * @param name имя анимации
     * @return объект анимации или null, если анимация не найдена
     */
    public Animation getAnimation(String name) {
        synchronized (animationLock) {
            return animations.get(name);
        }
    }
}