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
 * Scoreboard module refactored to use FastBoard API and ColorUtil.format everywhere
 */
public class ScoreboardModule extends AbstractModule implements Listener {

    private static final int UPDATE_INTERVAL;
    static {
        UPDATE_INTERVAL = 20; // default tick interval, overridden via config if set
    }

    private final Map<UUID, FastBoard> boards = new WeakHashMap<>();
    private BukkitRunnable updateTask;

    public ScoreboardModule(OriginChat plugin) {
        super(plugin, "scoreboard", "Scoreboard", "Customizes player scoreboard via FastBoard", "1.0");
    }

    @Override
    public void onEnable() {
        loadModuleConfig("modules/scoreboard");
        if (config == null) {
            log("Failed to load scoreboard config, using defaults.");
            config = plugin.getConfigManager().getMainConfig();
        }

        int interval = config.getInt("update_interval", UPDATE_INTERVAL);

        Bukkit.getPluginManager().registerEvents(this, plugin);

        Bukkit.getOnlinePlayers().forEach(this::createBoard);

        startUpdateTask(interval);
    }

    @Override
    public void onDisable() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        boards.values().forEach(FastBoard::delete);
        boards.clear();
    }

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

    private void createBoard(Player player) {
        FastBoard board = new FastBoard(player);
        boards.put(player.getUniqueId(), board);
        updateBoard(board);
    }

    private void updateBoard(FastBoard board) {
        Player player = board.getPlayer();

        // Получаем заголовок и полностью форматируем его
        String rawTitle = getLocalizedTitle(player);
        
        // Заменяем базовые плейсхолдеры
        String processedTitle = applyPlaceholders(player, rawTitle);
        
        // Применяем плейсхолдеры от других плагинов
        processedTitle = ColorUtil.setPlaceholders(player, processedTitle);
        
        // Используем прямой метод для HEX-цветов, если они есть в тексте
        if (processedTitle.contains("#")) {
            // Заменяем & на § для цветовых кодов
            processedTitle = processedTitle.replace('&', '§');
            // Обрабатываем HEX в формат §x§R§R§G§G§B§B
            processedTitle = ColorUtil.processHexColorsToLegacy(processedTitle);
        } else {
            // Используем полный формат для заголовка с поддержкой всех типов цветов
            processedTitle = ColorUtil.format(player, processedTitle, true, true, true);
        }
        
        board.updateTitle(processedTitle);

        // Обрабатываем строки аналогичным образом
        List<String> rawLines = getLocalizedLines(player);
        String[] lines = rawLines.stream()
            .map(line -> {
                // Заменяем базовые плейсхолдеры
                String processedLine = applyPlaceholders(player, line);
                
                // Применяем плейсхолдеры от других плагинов
                processedLine = ColorUtil.setPlaceholders(player, processedLine);
                
                // Обрабатываем HEX-цвета специальным образом, если они есть
                if (processedLine.contains("#")) {
                    // Заменяем & на § для цветовых кодов
                    processedLine = processedLine.replace('&', '§');
                    // Обрабатываем HEX в формат §x§R§R§G§G§B§B для корректного отображения в скорборде
                    processedLine = ColorUtil.processHexColorsToLegacy(processedLine);
                } else {
                    // Используем полный формат для строк
                    processedLine = ColorUtil.format(player, processedLine, true, true, true);
                }
                
                return processedLine;
            })
            .toArray(String[]::new);
        board.updateLines(lines);
    }

    private String getLocalizedTitle(Player player) {
        String title = getMessage(player, "title");
        if (title == null || title.startsWith("§cMessage not found")) {
            return "&e&lOriginChat"; // Вернем текст без форматирования, форматирование будет применено позже
        }
        return title;
    }

    private List<String> getLocalizedLines(Player player) {
        List<String> lines = getMessageList(player, "lines");
        if (lines == null || lines.isEmpty()) {
            return List.of(
                "&7Имя: &f{player}",
                "&7Мир: &f{world}",
                "&7Онлайн: &f{online}/{max_online}",
                "&7Здоровье: &c{health}/{max_health}",
                "&7Уровень: &a{level}",
                "&7Позиция: &f{x}, {y}, {z}"
            );
        }
        return lines;
    }

    private String applyPlaceholders(Player player, String text) {
        if (text == null) return "";
        
        // Заменяем базовые плейсхолдеры без форматирования
        return text
            .replace("{player}", player.getName())
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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> createBoard(event.getPlayer()), 5L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        FastBoard board = boards.remove(event.getPlayer().getUniqueId());
        if (board != null) board.delete();
    }
}