package me.nagibatirowanie.originchat.commands;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.locale.LocaleManager;
import me.nagibatirowanie.originchat.utils.ColorUtil;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Plugin command manager
 */
public class CommandManager implements CommandExecutor, TabCompleter {

    private final OriginChat plugin;
    private LocaleManager localeManager;


    public CommandManager(OriginChat plugin) {
        this.plugin = plugin;
        this.localeManager = plugin.getLocaleManager();
        
        plugin.getCommand("originchat").setExecutor(this);
        plugin.getCommand("originchat").setTabCompleter(this);
        
        plugin.getCommand("translatetoggle").setExecutor(this);
        plugin.getCommand("translatetoggle").setTabCompleter(this);
        
        // Регистрируем команду очистки чата
        new ClearChatCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Обработка команды translatetoggle
        if (command.getName().equalsIgnoreCase("translatetoggle")) {
            
            if (!(sender instanceof org.bukkit.entity.Player)) {
                localeManager.sendMessage(sender, "commands.player_only_command");
                return true;
            }
            
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
                        
            boolean newState = plugin.getTranslateManager().toggleTranslate(player);
                        
            if (newState) {
                localeManager.sendMessage(sender, "commands.translate_enabled");
            } else {
                
                localeManager.sendMessage(sender, "commands.translate_disabled");
            }
            
            return true;
        }
        
        // Обработка команды originchat
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("originchat.admin")) {
                    localeManager.sendMessage(sender, "commands.no_permission");
                    return true;
                }
                
                // Перезагрузка плагина
                plugin.getConfigManager().loadConfigs();
                plugin.getModuleManager().unloadModules();
                plugin.getModuleManager().loadModules();
                localeManager.loadLocales(); // Перезагружаем локали
                plugin.getAnimationManager().reloadAnimations(); // Перезагружаем анимации
                
                localeManager.sendMessage(sender, "commands.plugin_reloaded");
                break;
                
            case "module":
                if (!sender.hasPermission("originchat.admin")) {
                    localeManager.sendMessage(sender, "commands.no_permission");
                    return true;
                }
                
                if (args.length < 2) {
                    localeManager.sendMessage(sender, "commands.module.usage");
                    return true;
                }
                
                handleModuleCommand(sender, args);
                break;
                
            case "animation":
                if (args.length < 2) {
                    sendAnimationHelp(sender);
                    return true;
                }
                
                handleAnimationCommand(sender, args);
                break;
                
            case "help":
                sendHelp(sender);
                break;
                
            default:
                localeManager.sendMessage(sender, "commands.unknown_command");
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // Для команды translatetoggle не нужны автодополнения
        if (command.getName().equalsIgnoreCase("translatetoggle")) {
            return completions;
        }
        
        // Автодополнения для команды originchat
        if (args.length == 1) {
            // Первый аргумент - подкоманды
            if (sender.hasPermission("originchat.admin")) {
                completions.addAll(Arrays.asList("reload", "module", "animation", "help"));
            } else {
                completions.add("help");
                if (sender.hasPermission("originchat.animation.list") || 
                    sender.hasPermission("originchat.animation.info") || 
                    sender.hasPermission("originchat.animation.preview") || 
                    sender.hasPermission("originchat.animation.reload")) {
                    completions.add("animation");
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("module")) {
            // Второй аргумент для команды module
            if (sender.hasPermission("originchat.admin")) {
                completions.addAll(Arrays.asList("list", "enable", "disable", "reload"));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("module") 
                && (args[1].equalsIgnoreCase("enable") || args[1].equalsIgnoreCase("disable") || args[1].equalsIgnoreCase("reload"))) {
            // Третий аргумент - имя модуля
            if (sender.hasPermission("originchat.admin")) {
                plugin.getModuleManager().getModules().keySet().forEach(completions::add);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("animation")) {
            // Второй аргумент для команды animation
            if (sender.hasPermission("originchat.animation.reload")) {
                completions.add("reload");
            }
            if (sender.hasPermission("originchat.animation.list")) {
                completions.add("list");
            }
            if (sender.hasPermission("originchat.animation.info")) {
                completions.add("info");
            }
            if (sender.hasPermission("originchat.animation.preview") && sender instanceof org.bukkit.entity.Player) {
                completions.add("preview");
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("animation") 
                && (args[1].equalsIgnoreCase("info") || args[1].equalsIgnoreCase("preview"))) {
            // Третий аргумент - имя анимации
            if ((args[1].equalsIgnoreCase("info") && sender.hasPermission("originchat.animation.info")) ||
                (args[1].equalsIgnoreCase("preview") && sender.hasPermission("originchat.animation.preview"))) {
                completions.addAll(plugin.getAnimationManager().getAnimationNames());
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("animation") && args[1].equalsIgnoreCase("preview")) {
            // Больше не предлагаем варианты продолжительности для preview, так как теперь показываем все кадры сразу
            return new ArrayList<>();
        }
        
        return completions;
    }

    /**
     * Отправить справку по командам
     * @param sender отправитель
     */
    private void sendHelp(CommandSender sender) {
        // Используем новый метод для отправки списка сообщений
        localeManager.sendMessageList(sender, "commands.help.lines");
    }

    /**
     * Обработать команды для управления модулями
     * @param sender отправитель
     * @param args аргументы
     */
    private void handleModuleCommand(CommandSender sender, String[] args) {
        switch (args[1].toLowerCase()) {
            case "list":
                // Список всех модулей
                localeManager.sendMessageList(sender, "commands.module_list.lines");
                
                plugin.getModuleManager().getModules().forEach((id, module) -> {
                    boolean enabled = plugin.getModuleManager().isModuleEnabled(id);
                    String statusKey = enabled ? "commands.module_list.status_enabled" : "commands.module_list.status_disabled";
                    String locale = localeManager.getPlayerLocale(sender instanceof org.bukkit.entity.Player ? (org.bukkit.entity.Player) sender : null);
                    String status = localeManager.getMessage(statusKey, locale);

                    // Получаем локализованные имя и описание модуля с fallback
                    String localizedName = localeManager.getMessage("modules." + id + ".name", locale);
                    if (localizedName == null || localizedName.isEmpty() || localizedName.startsWith("modules.")) {
                        localizedName = module.getName();
                    }
                    String localizedDescription = localeManager.getMessage("modules." + id + ".description", locale);
                    if (localizedDescription == null || localizedDescription.isEmpty() || localizedDescription.startsWith("modules.")) {
                        localizedDescription = module.getDescription();
                    }

                    localeManager.sendMessage(sender, "commands.module_list.module_info", "{name}", localizedName, "{id}", id, "{status}", status);
                    localeManager.sendMessage(sender, "commands.module_list.module_description", "{description}", localizedDescription);
                });
                break;
                
            case "enable":
                if (args.length < 3) {
                    localeManager.sendMessage(sender, "commands.module.specify_module_enable");
                    return;
                }
                
                String moduleToEnable = args[2].toLowerCase();
                if (plugin.getModuleManager().enableModule(moduleToEnable)) {
                    localeManager.sendMessage(sender, "commands.module.module_enabled", "{module}", moduleToEnable);
                } else {
                    localeManager.sendMessage(sender, "commands.module.module_enable_failed", "{module}", moduleToEnable);
                }
                break;
                
            case "disable":
                if (args.length < 3) {
                    localeManager.sendMessage(sender, "commands.module.specify_module_disable");
                    return;
                }
                
                String moduleToDisable = args[2].toLowerCase();
                if (plugin.getModuleManager().disableModule(moduleToDisable)) {
                    localeManager.sendMessage(sender, "commands.module.module_disabled", "{module}", moduleToDisable);
                } else {
                    localeManager.sendMessage(sender, "commands.module.module_disable_failed", "{module}", moduleToDisable);
                }
                break;
                
            case "reload":
                if (args.length < 3) {
                    localeManager.sendMessage(sender, "commands.module.specify_module_reload");
                    return;
                }
                
                String moduleToReload = args[2].toLowerCase();
                if (plugin.getModuleManager().reloadModule(moduleToReload)) {
                    localeManager.sendMessage(sender, "commands.module.module_reloaded", "{module}", moduleToReload);
                } else {
                    localeManager.sendMessage(sender, "commands.module.module_reload_failed", "{module}", moduleToReload);
                }
                break;
                
            default:
                localeManager.sendMessage(sender, "commands.module.usage");
                break;
        }
    }

    /**
     * Отправить справку по командам анимации
     * @param sender отправитель
     */
    private void sendAnimationHelp(CommandSender sender) {
        localeManager.sendMessage(sender, "commands.animation.title");
        if (sender.hasPermission("originchat.animation.reload")) {
            sender.sendMessage("§f/originchat animation reload §7- §e" + localeManager.getMessage("commands.animation.reload", localeManager.getPlayerLocale(sender instanceof org.bukkit.entity.Player ? (org.bukkit.entity.Player) sender : null)));
        }
        if (sender.hasPermission("originchat.animation.list")) {
            sender.sendMessage("§f/originchat animation list §7- §e" + localeManager.getMessage("commands.animation.list", localeManager.getPlayerLocale(sender instanceof org.bukkit.entity.Player ? (org.bukkit.entity.Player) sender : null)));
        }
        if (sender.hasPermission("originchat.animation.info")) {
            sender.sendMessage("§f/originchat animation info <имя> §7- §e" + localeManager.getMessage("commands.animation.info", localeManager.getPlayerLocale(sender instanceof org.bukkit.entity.Player ? (org.bukkit.entity.Player) sender : null)));
        }
        if (sender.hasPermission("originchat.animation.preview") && sender instanceof org.bukkit.entity.Player) {
            sender.sendMessage("§f/originchat animation preview <имя> §7- §e" + localeManager.getMessage("commands.animation.preview", localeManager.getPlayerLocale(sender instanceof org.bukkit.entity.Player ? (org.bukkit.entity.Player) sender : null)));
        }
    }
    
    /**
     * Обработать команды для управления анимациями
     * @param sender отправитель
     * @param args аргументы
     */
    private void handleAnimationCommand(CommandSender sender, String[] args) {
        String locale = localeManager.getPlayerLocale(sender instanceof org.bukkit.entity.Player ? (org.bukkit.entity.Player) sender : null);
        
        switch (args[1].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("originchat.animation.reload")) {
                    localeManager.sendMessage(sender, "commands.animation.no_permission_reload");
                    return;
                }
                plugin.getAnimationManager().reloadAnimations();
                localeManager.sendMessage(sender, "commands.animation.reloaded");
                break;
                
            case "list":
                if (!sender.hasPermission("originchat.animation.list")) {
                    localeManager.sendMessage(sender, "commands.animation.no_permission_list");
                    return;
                }
                List<String> animations = plugin.getAnimationManager().getAnimationNames();
                if (animations.isEmpty()) {
                    localeManager.sendMessage(sender, "commands.animation.animations_not_found");
                } else {
                    localeManager.sendMessage(sender, "commands.animation.animations_list", "{count}", String.valueOf(animations.size()));
                    for (String name : animations) {
                        me.nagibatirowanie.originchat.animation.Animation animation = plugin.getAnimationManager().getAnimation(name);
                        sender.sendMessage("§a" + name + " §7- §f" + 
                            localeManager.getMessage("commands.animation.interval", locale).replace("{interval}", String.valueOf(animation.getInterval())) + 
                            "§f, " + 
                            localeManager.getMessage("commands.animation.frames_count", locale).replace("{count}", String.valueOf(animation.getFrames().size())));
                    }
                }
                break;
                
            case "info":
                if (!sender.hasPermission("originchat.animation.info")) {
                    localeManager.sendMessage(sender, "commands.animation.no_permission_info");
                    return;
                }
                if (args.length < 3) {
                    localeManager.sendMessage(sender, "commands.animation.specify_animation", "{command}", "info");
                    return;
                }
                String animName = args[2];
                me.nagibatirowanie.originchat.animation.Animation animation = plugin.getAnimationManager().getAnimation(animName);
                if (animation == null) {
                    localeManager.sendMessage(sender, "commands.animation.animation_not_found", "{name}", animName);
                    return;
                }
                localeManager.sendMessage(sender, "commands.animation.animation_info", "{name}", animName);
                localeManager.sendMessage(sender, "commands.animation.interval", "{interval}", String.valueOf(animation.getInterval()));
                localeManager.sendMessage(sender, "commands.animation.frames_count", "{count}", String.valueOf(animation.getFrames().size()));
                localeManager.sendMessage(sender, "commands.animation.current_frame", "{index}", String.valueOf(animation.getCurrentFrameIndex() + 1));
                localeManager.sendMessage(sender, "commands.animation.frames_title");
                int index = 0;
                for (String frame : animation.getFrames()) {
                    // Применяем форматирование цветов к кадрам
                    String formattedFrame = frame;
                    if (sender instanceof org.bukkit.entity.Player) {
                        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
                        // Заменяем плейсхолдер игрока, если он есть
                        if (formattedFrame.contains("{player}")) {
                            formattedFrame = formattedFrame.replace("{player}", player.getName());
                        }
                        formattedFrame = ColorUtil.format(player, formattedFrame);
                    } else {
                        formattedFrame = ColorUtil.format(formattedFrame);
                    }
                    String frameNumberMsg = localeManager.getMessage("commands.animation.frame_number", locale).replace("{number}", String.valueOf(index + 1));
                    sender.sendMessage("§7" + frameNumberMsg + "§r" + formattedFrame);
                    index++;
                }
                break;
                
            case "preview":
                if (!(sender instanceof org.bukkit.entity.Player)) {
                    localeManager.sendMessage(sender, "commands.animation.player_only");
                    return;
                }
                if (!sender.hasPermission("originchat.animation.preview")) {
                    localeManager.sendMessage(sender, "commands.animation.no_permission_preview");
                    return;
                }
                if (args.length < 3) {
                    localeManager.sendMessage(sender, "commands.animation.specify_animation", "{command}", "preview");
                    return;
                }
                String previewName = args[2];
                me.nagibatirowanie.originchat.animation.Animation previewAnim = plugin.getAnimationManager().getAnimation(previewName);
                if (previewAnim == null) {
                    localeManager.sendMessage(sender, "commands.animation.animation_not_found", "{name}", previewName);
                    return;
                }
                
                org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
                localeManager.sendMessage(sender, "commands.animation.preview_title", "{name}", previewName);
                localeManager.sendMessage(sender, "commands.animation.usage_text", "{placeholder}", "{animation_" + previewName + "}");
                
                // Выводим все кадры анимации сразу с правильным форматированием цветов
                localeManager.sendMessage(sender, "commands.animation.all_frames");
                List<String> frames = previewAnim.getFrames();
                for (int i = 0; i < frames.size(); i++) {
                    String frame = frames.get(i);
                    // Заменяем плейсхолдер игрока, если он есть
                    if (frame.contains("{player}")) {
                        frame = frame.replace("{player}", player.getName());
                    }
                    // Применяем форматирование цветов
                    frame = ColorUtil.format(player, frame);
                    String frameNumberMsg = localeManager.getMessage("commands.animation.frame_number", locale).replace("{number}", String.valueOf(i + 1));
                    sender.sendMessage("§7" + frameNumberMsg + "§r" + frame);
                }
                
                double seconds = previewAnim.getInterval() / 20.0;
                localeManager.sendMessage(sender, "commands.animation.interval_between", 
                    "{interval}", String.valueOf(previewAnim.getInterval()),
                    "{seconds}", String.format("%.1f", seconds));
                localeManager.sendMessage(sender, "commands.animation.total_frames", "{count}", String.valueOf(frames.size()));
                localeManager.sendMessage(sender, "commands.animation.usage_hint", "{placeholder}", "{animation_" + previewName + "}");
                break;
                
            default:
                sendAnimationHelp(sender);
                break;
        }
    }
}