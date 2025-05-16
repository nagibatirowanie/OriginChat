package me.nagibatirowanie.originchat.module.modules;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.commands.AfkCommand;
import me.nagibatirowanie.originchat.module.AbstractModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Модуль для управления режимом AFK (Away From Keyboard)
 */
public class AfkModule extends AbstractModule implements Listener {

    private boolean enabled;
    private boolean autoAfkEnabled;
    private int autoAfkTime; // время в секундах
    private boolean invulnerableToMobs;
    private boolean invulnerableToPlayers;
    private boolean mobsIgnoreAfk;
    
    private final Map<UUID, Boolean> afkPlayers = new HashMap<>();
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private final Map<UUID, Long> lastActivityTime = new HashMap<>();
    
    private AfkCommand afkCommand;
    private BukkitRunnable mobProtectionTask;

    public AfkModule(OriginChat plugin) {
        super(plugin, "afk", "AFK Module", "Adds AFK (Away From Keyboard) functionality", "1.0");
    }

    @Override
    public void onEnable() {
        loadModuleConfig("modules/afk");
        loadConfig();
        
        if (!enabled) {
            return;
        }
        
        // Регистрируем обработчик событий
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Регистрируем команду
        afkCommand = new AfkCommand(plugin, this);
        
        // Запускаем задачу проверки автоматического AFK
        if (autoAfkEnabled) {
            startAutoAfkTask();
        }
        
        // Запускаем задачу активной защиты от мобов
        if (mobsIgnoreAfk) {
            startMobProtectionTask();
        }
        
        log("AFK module has been successfully enabled.");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        
        // Останавливаем задачу защиты от мобов
        if (mobProtectionTask != null) {
            mobProtectionTask.cancel();
            mobProtectionTask = null;
        }
        
        // Отключаем режим AFK у всех игроков
        for (UUID uuid : new HashMap<>(afkPlayers).keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                setAfk(player, false);
            }
        }
        
        afkPlayers.clear();
        lastLocations.clear();
        lastActivityTime.clear();
        
        // Отменяем регистрацию команды
        if (afkCommand != null) {
            afkCommand.unregister();
            afkCommand = null;
        }
        
        log("AFK module has been disabled.");
    }

    /**
     * Загружает настройки из конфигурации
     */
    private void loadConfig() {
        try {
            enabled = config.getBoolean("enabled", true);
            autoAfkEnabled = config.getBoolean("auto-afk.enabled", true);
            autoAfkTime = config.getInt("auto-afk.time", 300); // 5 минут по умолчанию
            invulnerableToMobs = config.getBoolean("invulnerable.mobs", true);
            invulnerableToPlayers = config.getBoolean("invulnerable.players", false);
            mobsIgnoreAfk = config.getBoolean("mobs-ignore-afk", true);
            
            log("Configuration loaded successfully.");
        } catch (Exception e) {
            log("❗ Error loading AFK module configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Запускает задачу проверки автоматического AFK
     */
    private void startAutoAfkTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                
                // Пропускаем игроков, которые уже в AFK
                if (isAfk(player)) {
                    continue;
                }
                
                // Проверяем время последней активности
                if (lastActivityTime.containsKey(uuid)) {
                    long lastActivity = lastActivityTime.get(uuid);
                    long idleTime = (currentTime - lastActivity) / 1000; // в секундах
                    
                    if (idleTime >= autoAfkTime) {
                        setAfk(player, true);
                    }
                } else {
                    // Инициализируем время активности для новых игроков
                    updateActivity(player);
                }
            }
        }, 20L, 20L); // Проверка каждую секунду
    }

    /**
     * Запускает задачу активной защиты от мобов для AFK игроков
     */
    private void startMobProtectionTask() {
        mobProtectionTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (isAfk(player)) {
                        // Активно убираем всех мобов, нацеленных на AFK игрока
                        clearMobTargets(player);
                    }
                }
            }
        };
        mobProtectionTask.runTaskTimer(plugin, 20L, 20L); // Запуск каждую секунду
    }

    /**
     * Очищает цели всех мобов-монстров, которые нацелены на данного игрока
     * @param player игрок, цели на которого нужно очистить
     */
    private void clearMobTargets(Player player) {
        // Проверяем все сущности в радиусе 32 блоков от игрока
        for (Entity entity : player.getNearbyEntities(32, 32, 32)) {
            if (entity instanceof Monster) {
                Monster monster = (Monster) entity;
                
                // Проверяем, нацелен ли моб на этого игрока
                if (monster.getTarget() != null && monster.getTarget().equals(player)) {
                    monster.setTarget(null);
                    //debug("Убрана цель моба " + monster.getType() + " с AFK игрока " + player.getName());
                }
            }
        }
    }

    /**
     * Обновляет время последней активности игрока
     * @param player игрок
     */
    public void updateActivity(Player player) {
        lastActivityTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Устанавливает статус AFK для игрока
     * @param player игрок
     * @param afk новый статус AFK
     */
    public void setAfk(Player player, boolean afk) {
        UUID uuid = player.getUniqueId();
        boolean wasAfk = isAfk(player);
        
        if (afk == wasAfk) {
            return; // Статус не изменился
        }
        
        afkPlayers.put(uuid, afk);
        
        if (afk) {
            // Игрок входит в режим AFK
            broadcastAfkStatus(player, true);
            
            // Немедленно очищаем все цели мобов при входе в режим AFK
            if (mobsIgnoreAfk) {
                clearMobTargets(player);
            }
        } else {
            // Игрок выходит из режима AFK
            broadcastAfkStatus(player, false);
            updateActivity(player);
        }
    }

    /**
     * Проверяет, находится ли игрок в режиме AFK
     * @param player игрок
     * @return true, если игрок в режиме AFK
     */
    public boolean isAfk(Player player) {
        return afkPlayers.getOrDefault(player.getUniqueId(), false);
    }

    /**
     * Отправляет всем игрокам сообщение о смене статуса AFK
     * @param player игрок, изменивший статус
     * @param afk новый статус AFK
     */
    private void broadcastAfkStatus(Player player, boolean afk) {
        String messageKey = afk ? "modules.afk.messages.player_now_afk" : "modules.afk.messages.player_no_longer_afk";
        
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            String locale = plugin.getLocaleManager().getPlayerLocale(onlinePlayer);
            String message = plugin.getLocaleManager().getMessage(messageKey, locale)
                    .replace("{player}", player.getName());
            onlinePlayer.sendMessage(message);
        }
    }

    /**
     * Обработчик события входа игрока на сервер
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        updateActivity(player);
        lastLocations.put(player.getUniqueId(), player.getLocation());
    }

    /**
     * Обработчик события выхода игрока с сервера
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        afkPlayers.remove(uuid);
        lastLocations.remove(uuid);
        lastActivityTime.remove(uuid);
    }

    /**
     * Обработчик события движения игрока
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        
        // Проверяем, было ли реальное движение (изменение координат)
        if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
            // Обновляем время активности
            updateActivity(player);
            lastLocations.put(player.getUniqueId(), to);
            
            // Если игрок был в AFK и двинулся, выводим его из режима AFK
            if (isAfk(player) && autoAfkEnabled) {
                setAfk(player, false);
            }
        }
    }

    /**
     * Обработчик события получения урона игроком от любого источника
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        // Проверяем, является ли цель игроком в режиме AFK
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            
            if (isAfk(player)) {
                // Если это урон от сущности
                if (event instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent entityEvent = (EntityDamageByEntityEvent) event;
                    Entity damager = entityEvent.getDamager();
                    
                    // Проверяем настройки защиты от урона
                    if ((damager instanceof Monster && invulnerableToMobs) || 
                        (damager instanceof Player && invulnerableToPlayers)) {
                        event.setCancelled(true);
                        //debug("AFK игрок " + player.getName() + " защищен от урона от " + damager.getType());
                    }
                } else if (invulnerableToMobs) {
                    // Если это другой тип урона и включена защита от мобов, то защищаем и от других типов урона
                    event.setCancelled(true);
                    //debug("AFK игрок " + player.getName() + " защищен от урона типа " + event.getCause());
                }
            }
        }
    }

    /**
     * Обработчик события нацеливания моба на игрока
     * Используется в более старых версиях Minecraft
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        // Проверяем, является ли цель игроком в режиме AFK
        if (event.getTarget() instanceof Player && mobsIgnoreAfk) {
            Player player = (Player) event.getTarget();
            
            if (isAfk(player)) {
                // Отменяем событие и явно сбрасываем цель
                event.setCancelled(true);
                event.setTarget(null);
                
                // Если сущность - монстр, явно сбрасываем его цель
                if (event.getEntity() instanceof Monster) {
                    ((Monster) event.getEntity()).setTarget(null);
                }
                
                // Логируем для отладки
               //debug("Моб " + event.getEntity().getType() + " попытался атаковать AFK игрока " + player.getName() + ", атака предотвращена");
            }
        }
    }
    
    /**
     * Обработчик события нацеливания моба на живую сущность (игрока)
     * Используется в более новых версиях Minecraft
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        // Проверяем, является ли цель игроком в режиме AFK
        if (event.getTarget() instanceof Player && mobsIgnoreAfk) {
            Player player = (Player) event.getTarget();
            
            if (isAfk(player)) {
                // Отменяем событие и явно сбрасываем цель
                event.setCancelled(true);
                event.setTarget(null);
                
                // Если сущность - монстр, явно сбрасываем его цель
                if (event.getEntity() instanceof Monster) {
                    ((Monster) event.getEntity()).setTarget(null);
                }
                
                // Логируем для отладки
                //debug("Моб " + event.getEntity().getType() + " попытался атаковать AFK игрока " + player.getName() + ", атака предотвращена");
            }
        }
    }
}