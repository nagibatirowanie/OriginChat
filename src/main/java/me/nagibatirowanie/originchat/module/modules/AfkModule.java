/*
 * This file is part of OriginChat, a Minecraft plugin.
 *
 * Copyright (c) 2025 nagibatirowanie
 *
 * OriginChat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this plugin. If not, see <https://www.gnu.org/licenses/>.
 *
 * Created with ❤️ for the Minecraft community.
 */

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
 * Module for managing AFK (Away From Keyboard) status.
 */
public class AfkModule extends AbstractModule implements Listener {

    private boolean enabled;
    private boolean autoAfkEnabled;
    private int autoAfkTime; // time in seconds
    private boolean invulnerableToMobs;
    private boolean invulnerableToPlayers;
    private boolean mobsIgnoreAfk;

    private final Map<UUID, Boolean> afkPlayers = new HashMap<>();
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private final Map<UUID, Long> lastActivityTime = new HashMap<>();

    private AfkCommand afkCommand;
    private BukkitRunnable mobProtectionTask;

    /**
     * Constructs the AFK module.
     * @param plugin The main plugin instance.
     */
    public AfkModule(OriginChat plugin) {
        super(plugin, "afk", "AFK Module", "Adds AFK (Away From Keyboard) functionality", "1.0");
    }

    /**
     * Called when the module is enabled.
     */
    @Override
    public void onEnable() {
        loadModuleConfig("modules/afk");
        loadConfig();

        if (!enabled) {
            return;
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        afkCommand = new AfkCommand(plugin, this);

        if (autoAfkEnabled) {
            startAutoAfkTask();
        }

        if (mobsIgnoreAfk) {
            startMobProtectionTask();
        }

        log("AFK module has been successfully enabled.");
    }

    /**
     * Called when the module is disabled.
     */
    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);

        if (mobProtectionTask != null) {
            mobProtectionTask.cancel();
            mobProtectionTask = null;
        }

        // Set AFK status to false for all players
        for (UUID uuid : new HashMap<>(afkPlayers).keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                setAfk(player, false);
            }
        }

        afkPlayers.clear();
        lastLocations.clear();
        lastActivityTime.clear();

        if (afkCommand != null) {
            afkCommand.unregister();
            afkCommand = null;
        }

        log("AFK module has been disabled.");
    }

    /**
     * Loads the module's configuration.
     */
    private void loadConfig() {
        try {
            enabled = config.getBoolean("enabled", true);
            autoAfkEnabled = config.getBoolean("auto-afk.enabled", true);
            autoAfkTime = config.getInt("auto-afk.time", 300); // 5 minutes by default
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
     * Starts the automatic AFK check task.
     */
    private void startAutoAfkTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();

                if (isAfk(player)) {
                    continue;
                }

                if (lastActivityTime.containsKey(uuid)) {
                    long lastActivity = lastActivityTime.get(uuid);
                    long idleTime = (currentTime - lastActivity) / 1000; // in seconds

                    // Проверяем, не двигался ли игрок указанное время
                    if (idleTime >= autoAfkTime) {
                        // Устанавливаем статус AFK автоматически (manual = false)
                        setAfk(player, true, false);
                    }
                } else {
                    // Initialize activity time for new players
                    updateActivity(player);
                }
            }
        }, 20L, 20L); // Check every second
    }

    /**
     * Starts the active mob protection task for AFK players.
     */
    private void startMobProtectionTask() {
        mobProtectionTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (isAfk(player)) {
                        // Actively clear targets of all mobs targeting the AFK player
                        clearMobTargets(player);
                    }
                }
            }
        };
        mobProtectionTask.runTaskTimer(plugin, 20L, 20L); // Run every second
    }

    /**
     * Clears the targets of all hostile mobs that are targeting the specified player.
     * @param player The player whose targets should be cleared.
     */
    private void clearMobTargets(Player player) {
        for (Entity entity : player.getNearbyEntities(32, 32, 32)) {
            if (entity instanceof Monster) {
                Monster monster = (Monster) entity;

                if (monster.getTarget() != null && monster.getTarget().equals(player)) {
                    monster.setTarget(null);
                }
            }
        }
    }

    /**
     * Updates the player's last activity time.
     * @param player The player to update activity for.
     */
    public void updateActivity(Player player) {
        lastActivityTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Sets the AFK status for a player.
     * @param player The player to set AFK status for.
     * @param afk The new AFK status (true for AFK, false for not AFK).
     * @param manual Whether this change was triggered manually (true) or automatically (false).
     */
    public void setAfk(Player player, boolean afk, boolean manual) {
        UUID uuid = player.getUniqueId();
        boolean wasAfk = isAfk(player);

        if (afk == wasAfk) {
            return; // Status did not change
        }

        afkPlayers.put(uuid, afk);
        
        if (afk) {
            // Сохраняем информацию о том, как игрок попал в режим AFK
            manualAfk.put(uuid, manual);
            
            // Player enters AFK mode
            broadcastAfkStatus(player, true);

            // Immediately clear all mob targets upon entering AFK
            if (mobsIgnoreAfk) {
                clearMobTargets(player);
            }
        } else {
            // Player leaves AFK mode
            broadcastAfkStatus(player, false);
            updateActivity(player);
            
            // Удаляем информацию о способе входа в режим AFK
            manualAfk.remove(uuid);
        }
    }
    
    /**
     * Sets the AFK status for a player (backward compatibility).
     * @param player The player to set AFK status for.
     * @param afk The new AFK status (true for AFK, false for not AFK).
     */
    public void setAfk(Player player, boolean afk) {
        // По умолчанию считаем, что изменение статуса происходит вручную
        setAfk(player, afk, true);
    }

    /**
     * Checks if a player is currently in AFK mode.
     * @param player The player to check.
     * @return true if the player is in AFK mode, false otherwise.
     */
    public boolean isAfk(Player player) {
        return afkPlayers.getOrDefault(player.getUniqueId(), false);
    }

    /**
     * Broadcasts a message to all online players about a player's AFK status change.
     * @param player The player whose status changed.
     * @param afk The new AFK status (true if now AFK, false if no longer AFK).
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
     * Handles the PlayerJoinEvent to initialize AFK-related data for the player.
     * @param event The PlayerJoinEvent.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        updateActivity(player);
        lastLocations.put(player.getUniqueId(), player.getLocation());
    }

    /**
     * Handles the PlayerQuitEvent to remove AFK-related data for the player.
     * @param event The PlayerQuitEvent.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        afkPlayers.remove(uuid);
        lastLocations.remove(uuid);
        lastActivityTime.remove(uuid);
    }

    // Хранение информации о том, как игрок попал в режим AFK (автоматически или вручную)
    private final Map<UUID, Boolean> manualAfk = new HashMap<>();
    
    /**
     * Handles the PlayerMoveEvent to update activity and check for leaving AFK status.
     * @param event The PlayerMoveEvent.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        // Check if it was a significant movement (changed coordinates)
        if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
            // Обновляем последнюю локацию игрока
            lastLocations.put(player.getUniqueId(), to);
            
            UUID uuid = player.getUniqueId();
            boolean isPlayerAfk = isAfk(player);
            
            // Если игрок в режиме AFK и был установлен в этот режим автоматически,
            // то выводим его из режима AFK при движении
            if (isPlayerAfk && !manualAfk.getOrDefault(uuid, true)) {
                setAfk(player, false);
            }
            
            // Обновляем время активности только если игрок НЕ в режиме AFK
            if (!isPlayerAfk) {
                updateActivity(player);
            }
        }
    }

    /**
     * Handles the EntityDamageEvent to prevent damage to AFK players based on configuration.
     * @param event The EntityDamageEvent.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        // Check if the target is a player in AFK mode
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (isAfk(player)) {
                // If damage is from an entity
                if (event instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent entityEvent = (EntityDamageByEntityEvent) event;
                    Entity damager = entityEvent.getDamager();

                    // Check damage protection settings
                    if ((damager instanceof Monster && invulnerableToMobs) ||
                        (damager instanceof Player && invulnerableToPlayers)) {
                        event.setCancelled(true);
                    }
                } else if (invulnerableToMobs) {
                    // If it's another damage type and mob invulnerability is enabled, protect
                    event.setCancelled(true);
                }
            }
        }
    }

    /**
     * Handles the EntityTargetEvent to prevent mobs from targeting AFK players (for older versions).
     * @param event The EntityTargetEvent.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        // Check if the target is a player in AFK mode
        if (event.getTarget() instanceof Player && mobsIgnoreAfk) {
            Player player = (Player) event.getTarget();

            if (isAfk(player)) {
                // Cancel the event and explicitly unset the target
                event.setCancelled(true);
                event.setTarget(null);

                // If the entity is a monster, explicitly unset its target
                if (event.getEntity() instanceof Monster) {
                    ((Monster) event.getEntity()).setTarget(null);
                }
            }
        }
    }

    /**
     * Handles the EntityTargetLivingEntityEvent to prevent mobs from targeting AFK players (for newer versions).
     * @param event The EntityTargetLivingEntityEvent.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        // Check if the target is a player in AFK mode
        if (event.getTarget() instanceof Player && mobsIgnoreAfk) {
            Player player = (Player) event.getTarget();

            if (isAfk(player)) {
                // Cancel the event and explicitly unset the target
                event.setCancelled(true);
                event.setTarget(null);

                // If the entity is a monster, explicitly unset its target
                if (event.getEntity() instanceof Monster) {
                    ((Monster) event.getEntity()).setTarget(null);
                }
            }
        }
    }
}