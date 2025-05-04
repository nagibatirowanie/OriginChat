package me.nagibatirowanie.originchat.module.modules;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.module.AbstractModule;
import me.nagibatirowanie.originchat.utils.ColorUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Модуль для настройки таба сервера с поддержкой сортировки игроков по весам LuckPerms
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
        super(plugin, "tab", "Модуль таба", "Настройка таба сервера с поддержкой сортировки игроков", "1.0");
    }

    @Override
    public void onEnable() {
        // Загрузка конфигурации
        loadModuleConfig("modules/tab");
        if (config == null) {
            config = plugin.getConfigManager().getMainConfig();
        }
        
        loadConfig();
        
        if (!enabled) {
            log("Модуль таба отключен в конфигурации. Пропускаем активацию.");
            return;
        }
        
        setupLuckPerms();
        setupScoreboard();
        setupTeams();
        startUpdateTask();
        
        log("Модуль таба успешно включен");
    }

    /**
     * Настройка скорборда для управления командами
     */
    private void setupScoreboard() {
        scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard();
    }

    /**
     * Загрузить настройки из конфига
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
            log("Конфигурация успешно загружена");
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при загрузке конфигурации TabModule: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Настройка LuckPerms
     */
    private void setupLuckPerms() {
        if ("luckperms".equalsIgnoreCase(prioritySortingType)) {
            try {
                luckPerms = Bukkit.getServicesManager().getRegistration(LuckPerms.class).getProvider();
                if (luckPerms == null) {
                    log("LuckPerms не найден! Используется сортировка по группам из конфигурации.");
                    prioritySortingType = "group";
                } else {
                    log("LuckPerms успешно подключен");
                }
            } catch (Exception e) {
                log("Ошибка при подключении LuckPerms: " + e.getMessage());
                prioritySortingType = "group";
            }
        }
    }

    /**
     * Создание команд для групп
     */
    private void setupTeams() {
        // Очищаем предыдущие команды
        clearTeams();
        
        // Создаем команды для групп в зависимости от режима сортировки
        if ("luckperms".equals(prioritySortingType) && luckPerms != null) {
            // Получаем все группы из LuckPerms и сортируем их по весу
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
                // Отключаем отображение команды рядом с ником
                team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
                
                // Сохраняем соответствие группы и команды
                groupTeamNames.put(group.getName().toLowerCase(), teamName);
                
                position++;
            }
        } else {
            // Создаем команды для групп из конфигурации
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
                // Отключаем отображение команды рядом с ником
                team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
                
                // Сохраняем соответствие группы и команды
                groupTeamNames.put(groupName.toLowerCase(), teamName);
                
                position++;
            }
        }
    }

    /**
     * Запуск задачи обновления таба
     */
    private void startUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllPlayers();
            }
        };
        updateTask.runTaskTimer(plugin, 0, 20); // Обновление каждую секунду
    }

    /**
     * Обновить таб для всех игроков
     */
    private void updateAllPlayers() {
        // Обновляем для каждого игрока
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Определяем группу игрока
            String groupName = getPlayerGroup(player);
            
            // Получаем команду для этой группы
            String teamName = groupTeamNames.get(groupName.toLowerCase());
            if (teamName != null) {
                Team team = scoreboard.getTeam(teamName);
                if (team != null) {
                    // Добавляем игрока в команду, если он еще не в ней
                    if (!team.hasEntry(player.getName())) {
                        // Сначала удаляем игрока из всех других команд
                        for (Team t : scoreboard.getTeams()) {
                            if (t.hasEntry(player.getName())) {
                                t.removeEntry(player.getName());
                            }
                        }
                        team.addEntry(player.getName());
                    }
                    
                    // Настраиваем отображение игрока в табе
                    String displayName = getCustomName(player);
                    player.setPlayerListName(displayName);
                }
            }
            
            // Применяем скорборд к игроку для сортировки
            player.setScoreboard(scoreboard);
            
            // Обновляем header и footer
            updatePlayerTab(player);
        }
    }
    
    /**
     * Получить группу игрока
     * @param player игрок
     * @return имя группы
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
                
                // Получаем основную группу пользователя
                QueryOptions queryOptions = luckPerms.getContextManager().getQueryOptions(player);
                String primaryGroup = user.getPrimaryGroup();
                
                // Ищем группу с наибольшим весом
                Optional<String> highestWeightGroup = user.getInheritedGroups(queryOptions).stream()
                        .max(Comparator.comparingInt(g -> g.getWeight().orElse(0)))
                        .map(Group::getName);
                
                return highestWeightGroup.orElse(primaryGroup);
            } catch (Exception e) {
                log("Ошибка при получении группы LuckPerms для " + player.getName() + ": " + e.getMessage());
                return "default";
            }
        } else {
            // Если используется режим group, ищем группу с наивысшим приоритетом
            return groupPriorities.entrySet().stream()
                    .filter(e -> player.hasPermission("group." + e.getKey()))
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("default");
        }
    }
    
    /**
     * Очистить все команды скорборда
     */
    private void clearTeams() {
        for (Team team : scoreboard.getTeams()) {
            team.unregister();
        }
        groupTeamNames.clear();
    }

    /**
     * Обновить таб для игрока
     * @param player игрок
     */
    private void updatePlayerTab(Player player) {
        String header = String.join("\n", headerLines);
        String footer = String.join("\n", footerLines);
        
        // Сначала заменяем базовые плейсхолдеры
        header = applyPlaceholders(player, header);
        footer = applyPlaceholders(player, footer);
        
        // Затем применяем форматирование с использованием ColorUtil
        header = ColorUtil.format(player, header);
        footer = ColorUtil.format(player, footer);
        
        player.setPlayerListHeaderFooter(header, footer);
    }

    /**
     * Получить пользовательское имя игрока
     * @param player игрок
     * @return пользовательское имя
     */
    private String getCustomName(Player player) {
        // Сначала заменяем базовые плейсхолдеры, затем применяем форматирование через ColorUtil
        String name = playerFormat;
        name = applyPlaceholders(player, name);
        name = ColorUtil.format(player, name);
        name = name.replace("{player}", player.getName());
        
        return name;
    }

    /**
     * Применить плейсхолдеры к тексту
     * @param player игрок
     * @param text текст
     * @return текст с замененными плейсхолдерами
     */
    private String applyPlaceholders(Player player, String text) {
        if (text == null) return "";
        
        // Заменяем базовые плейсхолдеры
        text = text.replace("{player}", player.getName());
        text = text.replace("{displayname}", player.getDisplayName());
        text = text.replace("{world}", player.getWorld().getName());
        text = text.replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        text = text.replace("{max_online}", String.valueOf(Bukkit.getMaxPlayers()));
        text = text.replace("{ping}", String.valueOf(player.getPing()));
        text = text.replace("{group}", getPlayerGroup(player));
        
        // Используем PlaceholderAPI через ColorUtil, если он установлен
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
        log("Модуль таба выключен");
    }

    /**
     * Сбросить таб для всех игроков
     */
    private void resetAllPlayerTabs() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setPlayerListHeaderFooter("", "");
            player.setPlayerListName(player.getName());
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }
}