package me.nagibatirowanie.originchat.commands;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.config.ConfigManager;
import me.nagibatirowanie.originchat.locale.LocaleManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Plugin command manager
 */
public class CommandManager implements CommandExecutor, TabCompleter {

    private final OriginChat plugin;
    private ConfigurationSection config;
    private ConfigManager configManager;
    private LocaleManager localeManager;


    public CommandManager(OriginChat plugin) {
        this.plugin = plugin;
        this.localeManager = plugin.getLocaleManager();
        
        plugin.getCommand("originchat").setExecutor(this);
        plugin.getCommand("originchat").setTabCompleter(this);

            }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
        
        if (args.length == 1) {
            // Первый аргумент - подкоманды
            if (sender.hasPermission("originchat.admin")) {
                completions.addAll(Arrays.asList("reload", "module", "help"));
            } else {
                completions.add("help");
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
}