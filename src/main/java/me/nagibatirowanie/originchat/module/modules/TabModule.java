package me.nagibatirowanie.originchat.module.modules;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.module.AbstractModule;
import me.nagibatirowanie.originchat.utils.ColorUtil;
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
 * Module for customizing the server tab with support for sorting players by weights LuckPerms
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


    private void setupScoreboard() {
        scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard();
    }


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
            log("Конфигурация успешно загружена");
        } catch (Exception e) {
            plugin.getLogger().severe("❗ Error loading the TabModule configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Customizing LuckPerms
     */
    private void setupLuckPerms() {
        if ("luckperms".equalsIgnoreCase(prioritySortingType)) {
            try {
                Class<?> lpClass = Class.forName("net.luckperms.api.LuckPerms");
                luckPerms = Bukkit.getServicesManager().getRegistration((Class<net.luckperms.api.LuckPerms>) lpClass).getProvider();
                if (luckPerms == null) {
                    log("LuckPerms не найден! Используется сортировка по группам из конфигурации.");
                    prioritySortingType = "group";
                } else {
                    log("LuckPerms успешно подключен");
                }
            } catch (ClassNotFoundException e) {
                log("LuckPerms не установлен на сервере. Используется сортировка по группам.");
                prioritySortingType = "group";
                luckPerms = null;
            } catch (Exception e) {
                log("Ошибка при подключении LuckPerms: " + e.getMessage());
                prioritySortingType = "group";
                luckPerms = null;
            }
        }
    }

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
     * Starting the tab update task
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
    
    private void clearTeams() {
        for (Team team : scoreboard.getTeams()) {
            team.unregister();
        }
        groupTeamNames.clear();
    }


    private void updatePlayerTab(Player player) {
        String header = String.join("\n", headerLines);
        String footer = String.join("\n", footerLines);
        
        header = applyPlaceholders(player, header);
        footer = applyPlaceholders(player, footer);

        header = ColorUtil.format(player, header);
        footer = ColorUtil.format(player, footer);
        
        player.setPlayerListHeaderFooter(header, footer);
    }


    private String getCustomName(Player player) {
        // Сначала заменяем базовые плейсхолдеры, затем применяем форматирование через ColorUtil
        String name = playerFormat;
        name = applyPlaceholders(player, name);
        name = ColorUtil.format(player, name);
        name = name.replace("{player}", player.getName());
        
        return name;
    }


    private String applyPlaceholders(Player player, String text) {
        if (text == null) return "";
        
        text = text.replace("{player}", player.getName());
        text = text.replace("{displayname}", player.getDisplayName());
        text = text.replace("{world}", player.getWorld().getName());
        text = text.replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        text = text.replace("{max_online}", String.valueOf(Bukkit.getMaxPlayers()));
        text = text.replace("{ping}", String.valueOf(player.getPing()));
        text = text.replace("{group}", getPlayerGroup(player));
        
        text = ColorUtil.setPlaceholders(player, text);
        
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

    private void resetAllPlayerTabs() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setPlayerListHeaderFooter("", "");
            player.setPlayerListName(player.getName());
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }
}