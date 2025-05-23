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
import me.nagibatirowanie.originchat.module.AbstractModule;
import me.nagibatirowanie.originchat.utils.FormatUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Module for customizing the server tab with support for sorting players by LuckPerms weights
 */
public class TabModule extends AbstractModule {
    private List<String> headerLines;
    private List<String> footerLines;
    private String playerFormat;
    private String prioritySortingType;
    private Map<String, Integer> groupPriorities;
    private LuckPerms luckPerms;
    private BukkitRunnable updateTask;
    private boolean enabled;
    private Scoreboard scoreboard;
    private Map<String, String> groupTeamNames = new HashMap<>();

    /**
     * Creates a new TabModule instance
     * 
     * @param plugin The OriginChat plugin instance
     */
    public TabModule(OriginChat plugin) {
        super(plugin, "tab", "Tab", "Customizes the player tab list", "1.0");
    }

    @Override
    public void onEnable() {
        loadModuleConfig("modules/tab");
        if (config == null) {
            config = plugin.getConfigManager().getMainConfig();
        }
        
        loadConfig();
        
        if (!enabled) {
            return;
        }
        
        setupLuckPerms();
        setupScoreboard();
        setupTeams();
        startUpdateTask();
    }

    /**
     * Creates a new scoreboard for tab list management
     */
    private void setupScoreboard() {
        scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard();
    }

    /**
     * Loads module configuration from config file
     */
    private void loadConfig() {
        try {
            enabled = config.getBoolean("enabled", true);
            headerLines = config.getStringList("header");
            footerLines = config.getStringList("footer");
            playerFormat = config.getString("player_format");
            prioritySortingType = config.getString("priority_sorting_type", "group").toLowerCase();
            groupPriorities = new HashMap<>();
            ConfigurationSection section = config.getConfigurationSection("group_priorities");
            if (section != null) {
                for (String group : section.getKeys(false)) {
                    groupPriorities.put(group.toLowerCase(), section.getInt(group));
                }
            }
            log("Configuration loaded successfully");
        } catch (Exception e) {
            plugin.getLogger().severe("❗ Error loading the TabModule configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sets up LuckPerms integration for group-based sorting
     */
    private void setupLuckPerms() {
        if ("luckperms".equalsIgnoreCase(prioritySortingType)) {
            try {
                Class<?> lpClass = Class.forName("net.luckperms.api.LuckPerms");
                luckPerms = Bukkit.getServicesManager().getRegistration((Class<net.luckperms.api.LuckPerms>) lpClass).getProvider();
                if (luckPerms == null) {
                    log("LuckPerms not found! Using group sorting from configuration.");
                    prioritySortingType = "group";
                } else {
                    log("LuckPerms connected successfully");
                }
            } catch (ClassNotFoundException e) {
                log("LuckPerms is not installed on the server. Using group sorting.");
                prioritySortingType = "group";
                luckPerms = null;
            } catch (Exception e) {
                log("Error connecting to LuckPerms: " + e.getMessage());
                prioritySortingType = "group";
                luckPerms = null;
            }
        }
    }

    /**
     * Sets up teams for scoreboard-based sorting
     */
    private void setupTeams() {
        clearTeams();
        
        if ("luckperms".equals(prioritySortingType) && luckPerms != null) {
            Collection<Group> groups = luckPerms.getGroupManager().getLoadedGroups();
            List<Group> sortedGroups = groups.stream()
                    .sorted(Comparator.<Group>comparingInt(g -> g.getWeight().orElse(0)).reversed())
                    .collect(Collectors.toList());
            int position = 0;
            for (Group group : sortedGroups) {
                String teamName = String.format("%03d_%s", position, group.getName());
                if (teamName.length() > 16) {
                    teamName = teamName.substring(0, 16);
                }
                Team team = scoreboard.registerNewTeam(teamName);
                team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.ALWAYS);
                team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
                groupTeamNames.put(group.getName().toLowerCase(), teamName);
                position++;
            }
        } else {
            List<Map.Entry<String, Integer>> sortedGroups = new ArrayList<>(groupPriorities.entrySet());
            sortedGroups.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
            
            int position = 0;
            for (Map.Entry<String, Integer> entry : sortedGroups) {
                String groupName = entry.getKey();
                String teamName = String.format("%03d_%s", position, groupName);
                if (teamName.length() > 16) {
                    teamName = teamName.substring(0, 16);
                }
                
                Team team = scoreboard.registerNewTeam(teamName);
                team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.ALWAYS);
                team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
                
                groupTeamNames.put(groupName.toLowerCase(), teamName);
                
                position++;
            }
        }
    }

    /**
     * Starts the tab update task
     */
    private void startUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllPlayers();
            }
        };
        updateTask.runTaskTimer(plugin, 0, 20);
    }

    /**
     * Updates tab list for all online players
     */
    private void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String groupName = getPlayerGroup(player);
            
            String teamName = groupTeamNames.get(groupName.toLowerCase());
            if (teamName != null) {
                Team team = scoreboard.getTeam(teamName);
                if (team != null) {
                    if (!team.hasEntry(player.getName())) {
                        for (Team t : scoreboard.getTeams()) {
                            if (t.hasEntry(player.getName())) {
                                t.removeEntry(player.getName());
                            }
                        }
                        team.addEntry(player.getName());
                    }
                    
                    String displayName = getCustomName(player);
                    player.setPlayerListName(displayName);
                }
            }
            
            player.setScoreboard(scoreboard);
            
            updatePlayerTab(player);
        }
    }
    
    /**
     * Gets the player's highest priority group
     * 
     * @param player The player to get the group for
     * @return The name of the player's highest priority group
     */
    private String getPlayerGroup(Player player) {
        if ("luckperms".equals(prioritySortingType) && luckPerms != null) {
            try {
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user == null) {
                    user = luckPerms.getUserManager().loadUser(player.getUniqueId()).join();
                    if (user == null) {
                        return "default";
                    }
                }
                QueryOptions queryOptions = luckPerms.getContextManager().getQueryOptions(player);
                String primaryGroup = user.getPrimaryGroup();
                Optional<String> highestWeightGroup = user.getInheritedGroups(queryOptions).stream()
                        .max(Comparator.comparingInt(g -> g.getWeight().orElse(0)))
                        .map(Group::getName);
                return highestWeightGroup.orElse(primaryGroup);
            } catch (Throwable e) {
                log("LuckPerms is not available or an error has occurred for " + player.getName() + ": " + e.getMessage());
                prioritySortingType = "group";
                luckPerms = null;
                return groupPriorities.entrySet().stream()
                        .filter(entry -> player.hasPermission("group." + entry.getKey()))
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("default");
            }
        } else {
            return groupPriorities.entrySet().stream()
                    .filter(e -> player.hasPermission("group." + e.getKey()))
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("default");
        }
    }
    
    /**
     * Clears all existing teams from the scoreboard
     */
    private void clearTeams() {
        for (Team team : scoreboard.getTeams()) {
            team.unregister();
        }
        groupTeamNames.clear();
    }

    /**
     * Updates tab header and footer for a specific player
     * 
     * @param player The player to update the tab for
     */
    private void updatePlayerTab(Player player) {
        List<String> localizedHeaderLines = plugin.getConfigManager().getLocalizedMessageList("tab", "header", player);
        List<String> localizedFooterLines = plugin.getConfigManager().getLocalizedMessageList("tab", "footer", player);
        
        if (localizedHeaderLines.isEmpty()) {
            localizedHeaderLines = headerLines;
        }
        
        if (localizedFooterLines.isEmpty()) {
            localizedFooterLines = footerLines;
        }
        
        String header = String.join("\n", localizedHeaderLines);
        String footer = String.join("\n", localizedFooterLines);
        
        header = applyPlaceholders(player, header);
        footer = applyPlaceholders(player, footer);
        
        player.sendPlayerListHeaderAndFooter(
            FormatUtil.format(player, header, true, true, true),
            FormatUtil.format(player, footer, true, true, true)
        );
    }

    /**
     * Gets the custom player name for tab list
     * 
     * @param player The player to get the custom name for
     * @return The formatted player name for tab list
     */
    private String getCustomName(Player player) {
        String name = playerFormat;
        name = applyPlaceholders(player, name);
        name = name.replace("{player}", player.getName());
        
        return FormatUtil.formatLegacy(player, name, true, true, true);

    }

    /**
     * Applies placeholders to a text string
     * 
     * @param player The player to apply placeholders for
     * @param text The text to apply placeholders to
     * @return The text with placeholders applied
     */
    private String applyPlaceholders(Player player, String text) {
        if (text == null) return "";
        
        text = text.replace("{player}", player.getName());
        text = text.replace("{displayname}", player.getDisplayName());
        text = text.replace("{world}", player.getWorld().getName());
        text = text.replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        text = text.replace("{max_online}", String.valueOf(Bukkit.getMaxPlayers()));
        text = text.replace("{ping}", String.valueOf(player.getPing()));
        text = text.replace("{group}", getPlayerGroup(player));
        
        text = FormatUtil.setPlaceholders(player, text);
        
        return text;
    }

    @Override
    public void onDisable() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        clearTeams();
        resetAllPlayerTabs();
    }

    /**
     * Resets tab display for all players to default
     */
    private void resetAllPlayerTabs() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setPlayerListHeaderFooter("", "");
            player.setPlayerListName(player.getName());
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }
}