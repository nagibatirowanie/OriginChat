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
 import org.bukkit.Bukkit;
 import org.bukkit.command.Command;
 import org.bukkit.command.CommandExecutor;
 import org.bukkit.command.CommandSender;
 import org.bukkit.command.PluginCommand;
 import org.bukkit.configuration.ConfigurationSection;
 import org.bukkit.entity.Player;
 
 import java.util.*;
 
 /**
  * Module for roleplay commands (me, do, try, etc.)
  */
 public class RoleplayModule extends AbstractModule implements CommandExecutor {
 
     private static final List<String> COMMANDS = Arrays.asList(
             "me", "gme", "do", "gdo", "try", "gtry", "todo", "gtodo", "roll", "groll", "ball", "gball"
     );
 
     private final Random random = new Random();
     private boolean registered = false;
 
     private final Map<String, Integer> commandRanges = new HashMap<>();
     private final Map<String, String> commandFormats = new HashMap<>();
     private List<String> magicBallAnswers = new ArrayList<>();
 
     /**
      * Creates a new RoleplayModule instance
      * 
      * @param plugin The main plugin instance
      */
     public RoleplayModule(OriginChat plugin) {
         super(plugin, "roleplay", "Roleplay Module", "Adds commands for roleplay (me, do, try, etc.)", "1.0");
     }
 
     @Override
     public void onEnable() {
         loadModuleConfig("modules/roleplay");
         if (!registered) {
             registerCommands();
             registered = true;
         }
         loadConfig();
         plugin.getPluginLogger().info("Roleplay module loaded!");
     }
 
     @Override
     public void onDisable() {
         if (registered) {
             unregisterCommands();
             registered = false;
         }
         plugin.getPluginLogger().info("Roleplay module disabled.");
     }
 
     /**
      * Loads settings from config file
      */
     private void loadConfig() {
         try {
             commandRanges.clear();
             commandFormats.clear();
             magicBallAnswers.clear();
             ConfigurationSection ranges = config.getConfigurationSection("command_ranges");
             if (ranges != null) {
                 for (String command : ranges.getKeys(false)) {
                     commandRanges.put(command, ranges.getInt(command, 100));
                 }
             }
             ConfigurationSection formats = config.getConfigurationSection("formats");
             if (formats != null) {
                 for (String command : formats.getKeys(false)) {
                     commandFormats.put(command, formats.getString(command, ""));
                 }
             }
             magicBallAnswers = config.getStringList("magic_ball_answers");
             if (magicBallAnswers.isEmpty()) {
                 magicBallAnswers = Arrays.asList(
                     "Yes", "No", "Maybe", "Very likely", "Unlikely",
                     "Definitely yes", "Definitely no", "Ask later", "Cannot say now"
                 );
                 config.set("magic_ball_answers", magicBallAnswers);
                 config.save(configName);
             }
         } catch (Exception e) {
             plugin.getPluginLogger().severe("❗ Error loading RoleplayModule settings: " + e.getMessage());
             e.printStackTrace();
         }
     }
 
     /**
      * Registers all commands defined in COMMANDS list
      */
     private void registerCommands() {
         for (String commandName : COMMANDS) {
             PluginCommand command = plugin.getCommand(commandName);
             if (command != null) {
                 command.setExecutor(this);
             } else {
                 plugin.getPluginLogger().warning("Command '" + commandName + "' not found in plugin.yml!");
             }
         }
     }
 
     /**
      * Unregisters all commands defined in COMMANDS list
      */
     private void unregisterCommands() {
         for (String commandName : COMMANDS) {
             PluginCommand command = plugin.getCommand(commandName);
             if (command != null) {
                 command.setExecutor(null);
             }
         }
     }
 
     /**
      * Handles roleplay command execution
      * 
      * @param sender The command sender
      * @param command The command being executed
      * @param label The alias used for the command
      * @param args The command arguments
      * @return true if the command was handled, false otherwise
      */
     @Override
     public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
         if (!(sender instanceof Player)) {
             String localizedMessage = plugin.getConfigManager().getLocalizedMessage("roleplay", "errors.not-a-player", (Player)null);
             sender.sendMessage(FormatUtil.format(localizedMessage));
             return true;
         }
         Player player = (Player) sender;
         String commandName = command.getName().toLowerCase();
         int range = commandRanges.getOrDefault(commandName, 100);
         boolean isGlobal = commandName.startsWith("g");
         if (args.length == 0 && requiresTextArgument(commandName)) {
             String errorKey = "errors.not-enough-arguments";
             String localizedMessage = plugin.getConfigManager().getLocalizedMessage("roleplay", errorKey, player);
             player.sendMessage(FormatUtil.format(player, localizedMessage));
             return true;
         }
         String message = String.join(" ", args);
         switch (commandName) {
             case "me":
             case "gme":
                 sendRoleplayMessage(player, range, isGlobal, commandName, message);
                 break;
             case "do":
             case "gdo":
                 sendRoleplayMessage(player, range, isGlobal, commandName, message);
                 break;
             case "try":
             case "gtry":
                 handleTryCommand(player, range, isGlobal, message);
                 break;
             case "todo":
             case "gtodo":
                 sendRoleplayMessage(player, range, isGlobal, commandName, message);
                 break;
             case "roll":
             case "groll":
                 handleRollCommand(player, range, isGlobal, args);
                 break;
             case "ball":
             case "gball":
                 handleBallCommand(player, range, isGlobal, message);
                 break;
             default:
                 return false;
         }
         return true;
     }
 
     /**
      * Handles try/gtry command execution
      * 
      * @param player The player executing the command
      * @param range The command range in blocks
      * @param isGlobal Whether this is a global command
      * @param message The message to display
      */
     private void handleTryCommand(Player player, int range, boolean isGlobal, String message) {
         boolean success = random.nextBoolean();
         // Get results list from localization
         List<String> resultsList = plugin.getConfigManager().getLocalizedMessageList("roleplay", "results", player);
         // Use first two elements for success/failure
         // If list is empty or has not enough elements, use default values
         String resultText = success ? 
                 (resultsList.size() > 0 ? resultsList.get(0) : "success") : 
                 (resultsList.size() > 1 ? resultsList.get(1) : "failure");
         
         String formatKey = success ? (isGlobal ? "gtry_success" : "try_success") : (isGlobal ? "gtry_failure" : "try_failure");
         sendRoleplayMessage(player, range, isGlobal, formatKey, message);
     }
 
     /**
      * Handles roll/groll command execution
      * 
      * @param player The player executing the command
      * @param range The command range in blocks
      * @param isGlobal Whether this is a global command
      * @param args Command arguments, where args[0] is min value and args[1] is max value
      */
     private void handleRollCommand(Player player, int range, boolean isGlobal, String[] args) {
         if (args.length < 2) {
             String localizedMessage = plugin.getConfigManager().getLocalizedMessage("roleplay", "errors.not-enough-arguments", player);
             player.sendMessage(FormatUtil.format(player, localizedMessage));
             return;
         }
         try {
             int min = Integer.parseInt(args[0]);
             int max = Integer.parseInt(args[1]);
             if (min > max) {
                 int temp = min;
                 min = max;
                 max = temp;
             }
             int result = random.nextInt(max - min + 1) + min;
             
             // Use localized format for roll/groll command
             String commandKey = isGlobal ? "groll" : "roll";
             String localizedFormat = plugin.getConfigManager().getLocalizedMessage("roleplay", "format." + commandKey, player);
             String formatted = localizedFormat.replace("{player}", player.getName())
                     .replace("{min}", String.valueOf(min))
                     .replace("{max}", String.valueOf(max))
                     .replace("{result}", String.valueOf(result));
             
             // Send message to all applicable players based on command type (local/global)
             if (isGlobal) {
                 for (Player target : Bukkit.getOnlinePlayers()) {
                     String targetFormat = plugin.getConfigManager().getLocalizedMessage("roleplay", "format." + commandKey, target);
                     String targetFormatted = targetFormat.replace("{player}", player.getName())
                             .replace("{min}", String.valueOf(min))
                             .replace("{max}", String.valueOf(max))
                             .replace("{result}", String.valueOf(result));
                     target.sendMessage(FormatUtil.format(target, targetFormatted));
                 }
             } else {
                 for (Player target : player.getWorld().getPlayers()) {
                     if (target.getLocation().distance(player.getLocation()) <= range) {
                         String targetFormat = plugin.getConfigManager().getLocalizedMessage("roleplay", "format." + commandKey, target);
                         String targetFormatted = targetFormat.replace("{player}", player.getName())
                                 .replace("{min}", String.valueOf(min))
                                 .replace("{max}", String.valueOf(max))
                                 .replace("{result}", String.valueOf(result));
                         target.sendMessage(FormatUtil.format(target, targetFormatted));
                     }
                 }
             }
         } catch (NumberFormatException e) {
             String localizedMessage = plugin.getConfigManager().getLocalizedMessage("roleplay", "errors.not-enough-arguments", player);
             player.sendMessage(FormatUtil.format(player, localizedMessage));
         }
     }
 
     /**
      * Handles ball/gball command execution
      * 
      * @param player The player executing the command
      * @param range The command range in blocks
      * @param isGlobal Whether this is a global command
      * @param message The question to answer
      */
     private void handleBallCommand(Player player, int range, boolean isGlobal, String message) {
         String commandKey = isGlobal ? "gball" : "ball";
         
         // Send message to all applicable players based on command type (local/global)
         if (isGlobal) {
             for (Player target : Bukkit.getOnlinePlayers()) {
                 // Get results list for each target player individually
                 List<String> targetResultsList = plugin.getConfigManager().getLocalizedMessageList("roleplay", "results", target);
                 
                 // Select answer in target player's language
                 String answer;
                 if (targetResultsList.size() > 2) {
                     // Select random answer from results list, starting from index 2
                     int randomIndex = random.nextInt(targetResultsList.size() - 2) + 2;
                     answer = targetResultsList.get(randomIndex);
                 } else {
                     // Use fallback answers list
                     answer = magicBallAnswers.get(random.nextInt(magicBallAnswers.size()));
                 }
                 
                 String localizedFormat = plugin.getConfigManager().getLocalizedMessage("roleplay", "format." + commandKey, target);
                 String formatted = localizedFormat.replace("{player}", player.getName())
                         .replace("{message}", message)
                         .replace("{result}", answer);
                 target.sendMessage(FormatUtil.format(target, formatted));
             }
         } else {
             for (Player target : player.getWorld().getPlayers()) {
                 if (target.getLocation().distance(player.getLocation()) <= range) {
                     // Get results list for each target player individually
                     List<String> targetResultsList = plugin.getConfigManager().getLocalizedMessageList("roleplay", "results", target);
                     
                     // Select answer in target player's language
                     String answer;
                     if (targetResultsList.size() > 2) {
                         // Select random answer from results list, starting from index 2
                         int randomIndex = random.nextInt(targetResultsList.size() - 2) + 2;
                         answer = targetResultsList.get(randomIndex);
                     } else {
                         // Use fallback answers list
                         answer = magicBallAnswers.get(random.nextInt(magicBallAnswers.size()));
                     }
                     
                     String localizedFormat = plugin.getConfigManager().getLocalizedMessage("roleplay", "format." + commandKey, target);
                     String formatted = localizedFormat.replace("{player}", player.getName())
                             .replace("{message}", message)
                             .replace("{result}", answer);
                     target.sendMessage(FormatUtil.format(target, formatted));
                 }
             }
         }
     }
 
     /**
      * Checks if a command requires text argument
      * 
      * @param commandName The command name to check
      * @return true if the command requires text argument, false otherwise
      */
     private boolean requiresTextArgument(String commandName) {
         return Arrays.asList("me", "gme", "do", "gdo", "try", "gtry", "todo", "gtodo", "ball", "gball").contains(commandName);
     }
 
     /**
      * Sends roleplay message to applicable players
      * 
      * @param player The player who executed the command
      * @param range The command range in blocks
      * @param isGlobal Whether this is a global command
      * @param format The message format key
      * @param message The message content
      */
     private void sendRoleplayMessage(Player player, int range, boolean isGlobal, String format, String message) {
         if (format == null || format.isEmpty()) {
             format = "{player}: {message}";
         }
         
         // Get correct format key from localization
         String formatKey = "format." + format;
         
         if (isGlobal) {
             for (Player target : Bukkit.getOnlinePlayers()) {
                 String localizedFormat = plugin.getConfigManager().getLocalizedMessage("roleplay", formatKey, target);
                 String formatted = localizedFormat.replace("{player}", player.getName()).replace("{message}", message);
                 target.sendMessage(FormatUtil.format(target, formatted));
             }
         } else {
             for (Player target : player.getWorld().getPlayers()) {
                 if (target.getLocation().distance(player.getLocation()) <= range) {
                     String localizedFormat = plugin.getConfigManager().getLocalizedMessage("roleplay", formatKey, target);
                     String formatted = localizedFormat.replace("{player}", player.getName()).replace("{message}", message);
                     target.sendMessage(FormatUtil.format(target, formatted));
                 }
             }
         }
     }
 }