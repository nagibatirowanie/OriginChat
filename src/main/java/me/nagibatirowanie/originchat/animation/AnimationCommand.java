package me.nagibatirowanie.originchat.animation;

import me.nagibatirowanie.originchat.OriginChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Команда для управления анимациями
 */
public class AnimationCommand implements CommandExecutor, TabCompleter {

    private final OriginChat plugin;
    
    public AnimationCommand(OriginChat plugin) {
        this.plugin = plugin;
        plugin.getCommand("animation").setExecutor(this);
        plugin.getCommand("animation").setTabCompleter(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("originchat.animation.reload")) {
                    sender.sendMessage("§cУ вас нет прав для перезагрузки анимаций");
                    return true;
                }
                plugin.getAnimationManager().reloadAnimations();
                sender.sendMessage("§aАнимации успешно перезагружены");
                return true;
                
            case "list":
                if (!sender.hasPermission("originchat.animation.list")) {
                    sender.sendMessage("§cУ вас нет прав для просмотра списка анимаций");
                    return true;
                }
                List<String> animations = plugin.getAnimationManager().getAnimationNames();
                if (animations.isEmpty()) {
                    sender.sendMessage("§eАнимации не найдены");
                } else {
                    sender.sendMessage("§eСписок анимаций (" + animations.size() + "):");
                    for (String name : animations) {
                        Animation animation = plugin.getAnimationManager().getAnimation(name);
                        sender.sendMessage("§a" + name + " §7- §fИнтервал: §e" + animation.getInterval() + " тиков§f, Кадров: §e" + animation.getFrames().size());
                    }
                }
                return true;
                
            case "info":
                if (!sender.hasPermission("originchat.animation.info")) {
                    sender.sendMessage("§cУ вас нет прав для просмотра информации об анимации");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cУкажите имя анимации: /animation info <имя>");
                    return true;
                }
                String animName = args[1];
                Animation animation = plugin.getAnimationManager().getAnimation(animName);
                if (animation == null) {
                    sender.sendMessage("§cАнимация '" + animName + "' не найдена");
                    return true;
                }
                sender.sendMessage("§eИнформация об анимации '" + animName + "':");
                sender.sendMessage("§fИнтервал: §e" + animation.getInterval() + " тиков §7(" + (animation.getInterval() / 20.0) + " сек)");
                
                // Проверяем, есть ли локализованные кадры
                List<String> availableLocales = animation.getAvailableLocales();
                if (availableLocales.size() > 1) {
                    sender.sendMessage("§fДоступные языки: §e" + String.join(", ", availableLocales));
                    
                    // Если отправитель - игрок, показываем кадры для его языка
                    String locale = Animation.DEFAULT_LOCALE;
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        locale = plugin.getLocaleManager().getPlayerLocale(player);
                        if (!animation.hasLocale(locale)) {
                            locale = Animation.DEFAULT_LOCALE;
                        }
                        sender.sendMessage("§fВаш язык: §e" + locale);
                    }
                    
                    List<String> frames = animation.getFramesForLocale(locale);
                    sender.sendMessage("§fКадров для языка '" + locale + "': §e" + frames.size());
                    sender.sendMessage("§fТекущий кадр: §e" + (animation.getCurrentFrameIndex() + 1));
                    sender.sendMessage("§fКадры для языка '" + locale + "':");
                    int index = 0;
                    for (String frame : frames) {
                        sender.sendMessage("§7" + (index + 1) + ". §f" + frame);
                        index++;
                    }
                } else {
                    // Стандартный вывод для анимаций без локализации
                    sender.sendMessage("§fКадров: §e" + animation.getFrames().size());
                    sender.sendMessage("§fТекущий кадр: §e" + (animation.getCurrentFrameIndex() + 1));
                    sender.sendMessage("§fКадры:");
                    int index = 0;
                    for (String frame : animation.getFrames()) {
                        sender.sendMessage("§7" + (index + 1) + ". §f" + frame);
                        index++;
                    }
                }
                return true;
                
            case "preview":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cЭта команда доступна только для игроков");
                    return true;
                }
                if (!sender.hasPermission("originchat.animation.preview")) {
                    sender.sendMessage("§cУ вас нет прав для предпросмотра анимаций");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cУкажите имя анимации: /animation preview <имя>");
                    return true;
                }
                String previewName = args[1];
                Animation previewAnim = plugin.getAnimationManager().getAnimation(previewName);
                if (previewAnim == null) {
                    sender.sendMessage("§cАнимация '" + previewName + "' не найдена");
                    return true;
                }
                
                Player player = (Player) sender;
                sender.sendMessage("§eПредпросмотр анимации '" + previewName + "':");
                sender.sendMessage("§fИспользование в тексте: §e{animation_" + previewName + "}");
                
                // Проверяем, есть ли локализованные кадры
                List<String> previewLocales = previewAnim.getAvailableLocales();
                
                // Получаем язык игрока
                String locale = plugin.getLocaleManager().getPlayerLocale(player);
                if (!previewAnim.hasLocale(locale)) {
                    locale = Animation.DEFAULT_LOCALE;
                }
                
                if (previewLocales.size() > 1) {
                    sender.sendMessage("§fДоступные языки: §e" + String.join(", ", previewLocales));
                    sender.sendMessage("§fВаш язык: §e" + locale);
                    sender.sendMessage("§fПоказаны кадры для языка: §e" + locale);
                }
                
                // Выводим все кадры анимации сразу с правильным форматированием цветов
                sender.sendMessage("§fВсе кадры анимации:");
                List<String> frames = previewAnim.getFramesForLocale(locale);
                for (int i = 0; i < frames.size(); i++) {
                    String frame = frames.get(i);
                    // Заменяем плейсхолдер игрока, если он есть
                    if (frame.contains("{player}")) {
                        frame = frame.replace("{player}", player.getName());
                    }
                    // Применяем форматирование цветов
                    frame = me.nagibatirowanie.originchat.utils.ColorUtil.format(player, frame);
                    sender.sendMessage("§7Кадр " + (i + 1) + ": §r" + frame);
                }
                
                sender.sendMessage("§fИнтервал между кадрами: §e" + previewAnim.getInterval() + " тиков §7(" + (previewAnim.getInterval() / 20.0) + " сек)");
                sender.sendMessage("§fВсего кадров: §e" + frames.size());
                sender.sendMessage("§7Для просмотра анимации в действии используйте текст с плейсхолдером {animation_" + previewName + "}");
                return true;
                
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§e=== Команды управления анимациями ===");
        if (sender.hasPermission("originchat.animation.reload")) {
            sender.sendMessage("§f/animation reload §7- §eПерезагрузить анимации");
        }
        if (sender.hasPermission("originchat.animation.list")) {
            sender.sendMessage("§f/animation list §7- §eПоказать список анимаций");
        }
        if (sender.hasPermission("originchat.animation.info")) {
            sender.sendMessage("§f/animation info <имя> §7- §eПоказать информацию об анимации");
        }
        if (sender.hasPermission("originchat.animation.preview") && sender instanceof Player) {
            sender.sendMessage("§f/animation preview <имя> §7- §eПоказать все кадры анимации с форматированием");
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("originchat.animation.reload")) {
                completions.add("reload");
            }
            if (sender.hasPermission("originchat.animation.list")) {
                completions.add("list");
            }
            if (sender.hasPermission("originchat.animation.info")) {
                completions.add("info");
            }
            if (sender.hasPermission("originchat.animation.preview") && sender instanceof Player) {
                completions.add("preview");
            }
            return completions;
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("preview"))) {
            if ((args[0].equalsIgnoreCase("info") && !sender.hasPermission("originchat.animation.info")) ||
                (args[0].equalsIgnoreCase("preview") && !sender.hasPermission("originchat.animation.preview"))) {
                return new ArrayList<>();
            }
            return plugin.getAnimationManager().getAnimationNames();
        } else if (args.length == 3 && args[0].equalsIgnoreCase("preview")) {
            // Больше не предлагаем варианты продолжительности для preview, так как теперь показываем все кадры сразу
            return new ArrayList<>();
        }
        return new ArrayList<>();
    }
}