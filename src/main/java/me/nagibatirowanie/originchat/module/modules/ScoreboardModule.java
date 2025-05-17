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

import fr.mrmicky.fastboard.FastBoard;
import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.module.AbstractModule;
import me.nagibatirowanie.originchat.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Module to manage and update a customizable scoreboard using the FastBoard API.
 */
public class ScoreboardModule extends AbstractModule implements Listener {

    private static final int DEFAULT_UPDATE_INTERVAL = 20;
    private final Map<UUID, FastBoard> boards = new WeakHashMap<>();
    private BukkitRunnable updateTask;

    /**
     * Constructs the ScoreboardModule and sets module metadata.
     *
     * @param plugin the main plugin instance
     */
    public ScoreboardModule(OriginChat plugin) {
        super(plugin, "scoreboard", "Scoreboard", "Customizes player scoreboard via FastBoard", "1.0");
    }

    /**
     * Initializes the module: loads configuration, registers events, creates boards for online players,
     * and starts the periodic update task.
     */
    @Override
    public void onEnable() {
        loadModuleConfig("modules/scoreboard");
        if (config == null) {
            log("Failed to load scoreboard config, using defaults.");
            config = plugin.getConfigManager().getMainConfig();
        }

        int interval = config.getInt("update_interval", DEFAULT_UPDATE_INTERVAL);

        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getOnlinePlayers().forEach(this::createBoard);
        startUpdateTask(interval);
    }

    /**
     * Disables the module: stops the update task and removes all boards.
     */
    @Override
    public void onDisable() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        boards.values().forEach(FastBoard::delete);
        boards.clear();
    }

    /**
     * Schedules a repeating task to update all scoreboards at the given interval.
     *
     * @param interval ticks between updates
     */
    private void startUpdateTask(int interval) {
        if (updateTask != null) {
            updateTask.cancel();
        }
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                boards.values().forEach(ScoreboardModule.this::updateBoard);
            }
        };
        updateTask.runTaskTimer(plugin, 0L, interval);
    }

    /**
     * Creates a new scoreboard for the given player and updates it immediately.
     *
     * @param player the player to create the board for
     */
    private void createBoard(Player player) {
        FastBoard board = new FastBoard(player);
        boards.put(player.getUniqueId(), board);
        updateBoard(board);
    }

    /**
     * Updates the title and lines of the given FastBoard based on player data and configuration.
     *
     * @param board the FastBoard to update
     */
    private void updateBoard(FastBoard board) {
        Player player = board.getPlayer();

        String title = applyColorFormatting(player, getLocalizedTitle(player));
        board.updateTitle(title);

        List<String> rawLines = getLocalizedLines(player);
        String[] formattedLines = rawLines.stream()
            .map(line -> applyColorFormatting(player, line))
            .toArray(String[]::new);
        board.updateLines(formattedLines);
    }

    /**
     * Retrieves and validates the title from configuration or returns a default.
     *
     * @param player the player for locale lookup
     * @return raw title string with placeholders
     */
    private String getLocalizedTitle(Player player) {
        String title = getMessage(player, "title");
        if (title == null || title.startsWith("§cMessage not found")) {
            return "&e&lOriginChat";
        }
        return title;
    }

    /**
     * Retrieves and validates the list of lines from configuration or returns defaults.
     *
     * @param player the player for locale lookup
     * @return list of raw lines with placeholders
     */
    private List<String> getLocalizedLines(Player player) {
        List<String> lines = getMessageList(player, "lines");
        if (lines == null || lines.isEmpty()) {
            return List.of(
                "&7Name: &f{player}",
                "&7World: &f{world}",
                "&7Online: &f{online}/{max_online}",
                "&7Health: &c{health}/{max_health}",
                "&7Level: &a{level}",
                "&7Position: &f{x}, {y}, {z}"
            );
        }
        return lines;
    }

    /**
     * Replaces placeholders and applies color formatting, including HEX support.
     *
     * @param player the player context for placeholders and colors
     * @param text   raw text containing placeholders and color codes
     * @return fully formatted text
     */
    private String applyColorFormatting(Player player, String text) {
        if (text == null) {
            return "";
        }

        String processed = applyPlaceholders(player, text);
        processed = ColorUtil.setPlaceholders(player, processed);

        if (processed.contains("#")) {
            processed = processed.replace('&', '§');
            return ColorUtil.processHexColorsToLegacy(processed);
        }

        return ColorUtil.format(player, processed, true, true, true);
    }

    /**
     * Replaces basic placeholders in the text with player and server values.
     *
     * @param player the player whose data will replace placeholders
     * @param text   text containing basic placeholders
     * @return text with placeholders replaced
     */
    private String applyPlaceholders(Player player, String text) {
        return text.replace("{player}", player.getName())
                   .replace("{displayname}", player.getDisplayName())
                   .replace("{world}", player.getWorld().getName())
                   .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                   .replace("{max_online}", String.valueOf(Bukkit.getMaxPlayers()))
                   .replace("{ping}", String.valueOf(player.getPing()))
                   .replace("{health}", String.valueOf((int) player.getHealth()))
                   .replace("{max_health}", String.valueOf((int) player.getMaxHealth()))
                   .replace("{food}", String.valueOf(player.getFoodLevel()))
                   .replace("{level}", String.valueOf(player.getLevel()))
                   .replace("{exp}", String.valueOf(player.getExp()))
                   .replace("{x}", String.valueOf((int) player.getLocation().getX()))
                   .replace("{y}", String.valueOf((int) player.getLocation().getY()))
                   .replace("{z}", String.valueOf((int) player.getLocation().getZ()));
    }

    /**
     * Creates a scoreboard shortly after a player joins.
     *
     * @param event the join event containing the player
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> createBoard(event.getPlayer()), 5L);
    }

    /**
     * Removes and deletes the scoreboard when a player quits.
     *
     * @param event the quit event containing the player
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        FastBoard board = boards.remove(event.getPlayer().getUniqueId());
        if (board != null) {
            board.delete();
        }
    }
}