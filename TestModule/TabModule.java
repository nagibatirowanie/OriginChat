package me.nagibatirowanie.originChat.Modules;

import me.nagibatirowanie.originChat.OriginChat;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static me.nagibatirowanie.originLib.Messages.replaceHexColors;
import static me.nagibatirowanie.originLib.Messages.applyPlaceholders;

public class TabModule extends Module {

    private List<String> headerLines;
    private List<String> footerLines;
    private boolean showPing;
    private String playerFormat;
    private String prioritySortingType;
    private Map<String, Integer> groupPriorities;
    private LuckPerms luckPerms;
    private BukkitRunnable updateTask;

    public TabModule(OriginChat plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        if (!isEnabled()) return;

        loadConfig();
        setupLuckPerms();
        startUpdateTask();
    }

    @Override
    protected void loadConfig() {
        try {
            headerLines = plugin.getConfig().getStringList("tab.header");
            footerLines = plugin.getConfig().getStringList("tab.footer");
            showPing = plugin.getConfig().getBoolean("tab.show_ping", true);
            playerFormat = plugin.getConfig().getString("tab.player_format", "&7[&b%player_prefix%&7] &f{player}");
            prioritySortingType = plugin.getConfig().getString("tab.priority_sorting_type", "group").toLowerCase();

            groupPriorities = new HashMap<>();
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("tab.group_priorities");
            if (section != null) {
                for (String group : section.getKeys(false)) {
                    groupPriorities.put(group.toLowerCase(), section.getInt(group));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load TabModule configuration: " + e.getMessage());
        }
    }

    private void setupLuckPerms() {
        if ("luckperms".equalsIgnoreCase(prioritySortingType)) {
            luckPerms = Bukkit.getServicesManager().load(LuckPerms.class);
            if (luckPerms == null) {
                plugin.getLogger().warning("LuckPerms not found! Using group sorting.");
                prioritySortingType = "group";
            }
        }
    }

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
        List<Player> sortedPlayers = getSortedPlayers();
        Map<Player, String> customNames = generateCustomNames(sortedPlayers);

        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerTab(player);
            updatePlayerListNames(player, customNames);
        }
    }

    private List<Player> getSortedPlayers() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        // Исправленная сортировка по убыванию
        players.sort((p1, p2) -> {
            int priority1 = getPriority(p1);
            int priority2 = getPriority(p2);
            return Integer.compare(priority2, priority1); // Правильное сравнение
        });

        return players;
    }

    private Map<Player, String> generateCustomNames(List<Player> sortedPlayers) {
        Map<Player, String> names = new HashMap<>();
        int position = 1;

        for (Player player : sortedPlayers) {
            String prefix = ChatColor.GRAY + "[" + position + "] ";
            names.put(player, prefix + getCustomName(player));
            position++;
        }
        return names;
    }

    private void updatePlayerTab(Player player) {
        String header = applyPlaceholders(player, String.join("\n", headerLines));
        String footer = applyPlaceholders(player, String.join("\n", footerLines));
        player.setPlayerListHeaderFooter(header, footer);
    }

    private void updatePlayerListNames(Player viewer, Map<Player, String> customNames) {
        for (Map.Entry<Player, String> entry : customNames.entrySet()) {
            Player target = entry.getKey();
            if (viewer.canSee(target)) {
                target.setPlayerListName(entry.getValue());
            }
        }
    }

    private String getCustomName(Player player) {
        String name = applyPlaceholders(player, playerFormat);
        if (showPing) {
            name += " " + ChatColor.GRAY + "[" + player.getPing() + "ms]";
        }
        return ChatColor.translateAlternateColorCodes('&', replaceHexColors(name));
    }

    private int getPriority(Player player) {
        if ("luckperms".equals(prioritySortingType) && luckPerms != null) {
            return getLuckPermsWeight(player);
        }
        return getGroupPriority(player);
    }

    private int getLuckPermsWeight(Player player) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return 0;

        return user.getInheritedGroups(luckPerms.getContextManager().getStaticQueryOptions())
                .stream()
                .mapToInt(group -> group.getWeight().orElse(0))
                .max()
                .orElse(0);
    }

    private int getGroupPriority(Player player) {
        return groupPriorities.entrySet().stream()
                .filter(e -> player.hasPermission("group." + e.getKey()))
                .mapToInt(Map.Entry::getValue)
                .max()
                .orElse(0);
    }

    @Override
    public void onDisable() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        resetAllPlayerTabs();
    }

    private void resetAllPlayerTabs() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setPlayerListHeaderFooter("", "");
            player.setPlayerListName(player.getName());
        }
    }
}