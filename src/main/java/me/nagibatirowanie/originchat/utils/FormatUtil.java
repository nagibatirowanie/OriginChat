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

 package me.nagibatirowanie.originchat.utils;

 import io.github.milkdrinkers.colorparser.ColorParser;
 import me.clip.placeholderapi.PlaceholderAPI;
 import me.nagibatirowanie.originchat.OriginChat;
 import me.nagibatirowanie.originchat.animation.AnimationManager;
 import net.kyori.adventure.text.Component;
 import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
 import org.bukkit.Bukkit;
 import org.bukkit.ChatColor;
 import org.bukkit.entity.Player;
 
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 /**
  * Enhanced utility class for text formatting using ColorParser
  * Supports standard Minecraft color codes, HEX colors, and MiniMessage
  */
 public class FormatUtil {
 
     // Using static ColorParser methods instead of creating an instance
     
     // Pattern for HEX colors in &#RRGGBB or #RRGGBB format
     private static final Pattern HEX_PATTERN = Pattern.compile("(&#|#)([A-Fa-f0-9]{6})");
     
     // Legacy serializer for converting Component back to legacy strings
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .hexColors()
            // Removed useUnusualXRepeatedCharacterHexFormat to avoid §x format issues
            .build();
     
     // Cache for checking PlaceholderAPI availability
     private static Boolean placeholderAPIEnabled = null;
 
     /**
      * Checks if PlaceholderAPI is installed and available
      * 
      * @return true if PlaceholderAPI is available
      */
     public static boolean isPlaceholderAPIEnabled() {
         if (placeholderAPIEnabled == null) {
             placeholderAPIEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
         }
         return placeholderAPIEnabled;
     }
 
     /**
      * Replaces PlaceholderAPI placeholders and animations in text
      * 
      * @param player Player for processing placeholders
      * @param text Text to process
      * @param enableAnimations Enable animation processing
      * @return Text with processed placeholders and animations
      */
     public static String setPlaceholders(Player player, String text, boolean enableAnimations) {
         if (text == null || text.isEmpty()) return "";
         
         // First process animations if enabled
         String processed = text;
         if (enableAnimations) {
             AnimationManager animationManager = OriginChat.getInstance().getAnimationManager();
             if (animationManager != null && processed.contains("{animation_")) {
                 processed = animationManager.processAnimations(processed, player);
             }
         }
         
         // Then process PlaceholderAPI placeholders if player is provided and PlaceholderAPI is enabled
         if (player != null && isPlaceholderAPIEnabled() && processed.contains("%")) {
             try {
                 // Process placeholders up to 10 times to handle nested placeholders
                 String prev;
                 String result = processed;
                 int maxIterations = 10;
                 int count = 0;
                 do {
                     prev = result;
                     result = PlaceholderAPI.setPlaceholders(player, prev);
                     count++;
                 } while (!result.equals(prev) && count < maxIterations);
                 return result;
             } catch (Exception e) {
                 Bukkit.getLogger().warning("Error processing placeholders: " + e.getMessage());
                 e.printStackTrace();
             }
         }
         return processed;
     }
     
     /**
      * Replaces PlaceholderAPI placeholders and animations in text
      * Animations are enabled by default
      * 
      * @param player Player for processing placeholders
      * @param text Text to process
      * @return Text with processed placeholders and animations
      */
     public static String setPlaceholders(Player player, String text) {
         return setPlaceholders(player, text, true);
     }
 
     /**
      * Removes all color codes from text
      * 
      * @param text Text to strip colors from
      * @return Text without color codes
      */
     public static String stripColors(String text) {
         if (text == null || text.isEmpty()) return "";
         return ChatColor.stripColor(text);
     }
     
     /**
      * Converts HEX colors in &#RRGGBB or #RRGGBB format to MiniMessage format <#RRGGBB>
      * 
      * @param text Text to process
      * @return Text with HEX colors converted to MiniMessage format
      */
     public static String convertHexToMiniMessage(String text) {
         if (text == null || text.isEmpty()) return "";
         
         Matcher matcher = HEX_PATTERN.matcher(text);
         StringBuffer result = new StringBuffer();
         
         while (matcher.find()) {
             String hexColor = matcher.group(2);
             matcher.appendReplacement(result, "<#" + hexColor + ">");
         }
         matcher.appendTail(result);
         
         return result.toString();
     }
 
     /**
      * Pre-processes text by converting HEX colors to MiniMessage format
      * This prevents ColorParser from converting them to legacy codes
      * 
      * @param text Text to preprocess
      * @param enableColors Whether colors should be processed
      * @return Preprocessed text
      */
     private static String preprocessText(String text, boolean enableColors) {
         if (text == null || text.isEmpty() || !enableColors) return text;
         
         // Convert HEX colors to MiniMessage format to prevent legacy conversion
         return convertHexToMiniMessage(text);
     }
 
     /**
      * Creates a component from a string with support for &-codes, hex and MiniMessage
      * 
      * @param text Text to convert
      * @param enableColors Allow formatting with color codes and hex colors
      * @param enableMiniMessage Allow MiniMessage formatting
      * @return Component with applied formatting
      */
     public static Component toComponent(String text, boolean enableColors, boolean enableMiniMessage) {
         if (text == null || text.isEmpty()) return Component.empty();
 
         // If formatting is disabled, return plain text
         if (!enableColors && !enableMiniMessage) {
             return Component.text(text);
         }
         
         try {
             // Pre-process HEX colors to MiniMessage format if colors are enabled
             String processedText = preprocessText(text, enableColors);
             
             // Use ColorParser for text processing
             if (enableMiniMessage) {
                 // If MiniMessage is enabled, use full processing
                 return ColorParser.of(processedText)
                         .parseLegacy() // Process &-codes 
                         .build();
             } else if (enableColors) {
                 // If only colors are enabled, process legacy codes and HEX
                 return ColorParser.of(processedText)
                         .parseLegacy() // Process &-codes
                         .build();
             } else {
                 // If nothing is enabled
                 return Component.text(processedText);
             }
         } catch (Exception e) {
             Bukkit.getLogger().warning("Error creating component via ColorParser: " + e.getMessage());
             return Component.text(stripColors(text));
         }
     }
     
     /**
      * Creates a component from a string with all formatting enabled
      * 
      * @param text Text to convert
      * @return Component with applied formatting
      */
     public static Component toComponent(String text) {
         return toComponent(text, true, true);
     }
 
     /**
      * Creates a component with player placeholder support
      * 
      * @param player Player for processing placeholders
      * @param text Text to convert
      * @param enableColors Allow formatting with color codes and hex colors
      * @param enableMiniMessage Allow MiniMessage formatting
      * @param enablePlaceholders Allow placeholder processing
      * @param enableAnimations Allow animation processing
      * @return Component with applied formatting
      */
     public static Component toComponent(Player player, String text, boolean enableColors, boolean enableMiniMessage, boolean enablePlaceholders, boolean enableAnimations) {
         if (text == null || text.isEmpty()) return Component.empty();
         
         // First process placeholders if enabled
         String processed = text;
         if (enablePlaceholders) {
             processed = setPlaceholders(player, text, enableAnimations);
         }
         
         // Then create a component with formatting
         return toComponent(processed, enableColors, enableMiniMessage);
     }
     
     /**
      * Creates a component with player placeholder support (animations disabled)
      * 
      * @param player Player for processing placeholders
      * @param text Text to convert
      * @param enableColors Allow formatting with color codes and hex colors
      * @param enableMiniMessage Allow MiniMessage formatting
      * @param enablePlaceholders Allow placeholder processing
      * @return Component with applied formatting
      */
     public static Component toComponent(Player player, String text, boolean enableColors, boolean enableMiniMessage, boolean enablePlaceholders) {
         return toComponent(player, text, enableColors, enableMiniMessage, enablePlaceholders, false);
     }
     
     /**
      * Creates a component with player placeholder support (all formatting enabled)
      * 
      * @param player Player for processing placeholders
      * @param text Text to convert
      * @return Component with applied formatting
      */
     public static Component toComponent(Player player, String text) {
         return toComponent(player, text, true, true, true, false);
     }
 
     /**
      * Formats text and returns an Adventure API component
      * 
      * @param text Text to format
      * @param enableColors Allow formatting with color codes and hex colors
      * @param enableMiniMessage Allow MiniMessage formatting
      * @param enablePlaceholders Allow placeholder processing
      * @param enableAnimations Allow animation processing
      * @return Formatted text as an Adventure API component
      */
     public static Component format(String text, boolean enableColors, boolean enableMiniMessage, boolean enablePlaceholders, boolean enableAnimations) {
         if (text == null || text.isEmpty()) return Component.empty();
         
         // Process placeholders if enabled (with any online player for global placeholders)
         String processed = text;
         if (enablePlaceholders && isPlaceholderAPIEnabled() && text.contains("%")) {
             Player anyPlayer = null;
             if (!Bukkit.getOnlinePlayers().isEmpty()) {
                 anyPlayer = Bukkit.getOnlinePlayers().iterator().next();
             }
             processed = setPlaceholders(anyPlayer, text, enableAnimations);
         }
         
         // If formatting is disabled, return plain text
         if (!enableColors && !enableMiniMessage) {
             return Component.text(processed);
         }
         
         // Pre-process HEX colors to MiniMessage format if colors are enabled
         processed = preprocessText(processed, enableColors);
         
         // Use ColorParser for text formatting
          try {
              if (enableMiniMessage) {
                   // If MiniMessage is enabled, use full processing
                   return ColorParser.of(processed)
                           .parseLegacy() // Process &-codes
                           .build();
               } else if (enableColors) {
                   // If only colors are enabled, process legacy codes and HEX
                   return ColorParser.of(processed)
                           .parseLegacy() // Process &-codes
                           .build();
              } else {
                  // If nothing is enabled
                  return Component.text(processed);
              }
         } catch (Exception e) {
             Bukkit.getLogger().warning("Error creating component via ColorParser: " + e.getMessage());
             return Component.text(stripColors(processed));
         }
     }
     
     /**
      * Formats text and returns an Adventure API component (animations disabled)
      * 
      * @param text Text to format
      * @param enableColors Allow formatting with color codes and hex colors
      * @param enableMiniMessage Allow MiniMessage formatting
      * @param enablePlaceholders Allow placeholder processing
      * @return Formatted text as an Adventure API component
      */
     public static Component format(String text, boolean enableColors, boolean enableMiniMessage, boolean enablePlaceholders) {
         return format(text, enableColors, enableMiniMessage, enablePlaceholders, false);
     }
     
     /**
      * Formats text with all formatting enabled (except animations) and returns a component
      * 
      * @param text Text to format
      * @return Formatted text as an Adventure API component
      */
     public static Component format(String text) {
         return format(text, true, true, true, false);
     }
 
     /**
      * Formats text for a specific player with placeholders and returns a component
      * 
      * @param player Player for processing placeholders
      * @param text Text to format
      * @param enableColors Allow formatting with color codes and hex colors
      * @param enableMiniMessage Allow MiniMessage formatting
      * @param enablePlaceholders Allow placeholder processing
      * @param enableAnimations Allow animation processing
      * @return Formatted text as an Adventure API component
      */
     public static Component format(Player player, String text, boolean enableColors, boolean enableMiniMessage, boolean enablePlaceholders, boolean enableAnimations) {
         if (text == null || text.isEmpty()) return Component.empty();
         
         // Process placeholders with player context if enabled
         String processed = text;
         if (enablePlaceholders) {
             processed = setPlaceholders(player, text, enableAnimations);
         }
         
         // If formatting is disabled, return plain text
         if (!enableColors && !enableMiniMessage) {
             return Component.text(processed);
         }
         
         // Pre-process HEX colors to MiniMessage format if colors are enabled
         processed = preprocessText(processed, enableColors);
         
         // Use ColorParser for text formatting
          try {
              if (enableMiniMessage) {
                   // If MiniMessage is enabled, use full processing
                   return ColorParser.of(processed)
                           .parseLegacy() // Process &-codes
                           .build();
               } else if (enableColors) {
                   // If only colors are enabled, process legacy codes and HEX
                   return ColorParser.of(processed)
                           .parseLegacy() // Process &-codes
                           .build();
              } else {
                  // If nothing is enabled
                  return Component.text(processed);
              }
         } catch (Exception e) {
             Bukkit.getLogger().warning("Error creating component via ColorParser: " + e.getMessage());
             return Component.text(stripColors(processed));
         }
     }
     
     /**
      * Formats text for a specific player (animations disabled) and returns a component
      * 
      * @param player Player for processing placeholders
      * @param text Text to format
      * @param enableColors Allow formatting with color codes and hex colors
      * @param enableMiniMessage Allow MiniMessage formatting
      * @param enablePlaceholders Allow placeholder processing
      * @return Formatted text as an Adventure API component
      */
     public static Component format(Player player, String text, boolean enableColors, boolean enableMiniMessage, boolean enablePlaceholders) {
         return format(player, text, enableColors, enableMiniMessage, enablePlaceholders, false);
     }
     
     /**
      * Formats text for a specific player with all formatting enabled (except animations) and returns a component
      * 
      * @param player Player for processing placeholders
      * @param text Text to format
      * @return Formatted text as an Adventure API component
      */
     public static Component format(Player player, String text) {
         return format(player, text, true, true, true, false);
     }
     
     /**
      * Formats text to a legacy string with colors and placeholders
      * 
      * @param text Text to format
      * @param enableColors Allow formatting with color codes and hex colors
      * @param enableMiniMessage Allow MiniMessage formatting
      * @param enablePlaceholders Allow placeholder processing
      * @param enableAnimations Allow animation processing
      * @return Formatted text as a legacy string
      */
     public static String formatLegacy(String text, boolean enableColors, boolean enableMiniMessage, boolean enablePlaceholders, boolean enableAnimations) {
         // Use the format method to get a component and then convert it to a legacy string
         Component component = format(text, enableColors, enableMiniMessage, enablePlaceholders, enableAnimations);
         return LEGACY_SERIALIZER.serialize(component);
     }
     
     /**
      * Formats text to a legacy string with colors and placeholders (animations disabled)
      * 
      * @param text Text to format
      * @param enableColors Allow formatting with color codes and hex colors
      * @param enableMiniMessage Allow MiniMessage formatting
      * @param enablePlaceholders Allow placeholder processing
      * @return Formatted text as a legacy string
      */
     public static String formatLegacy(String text, boolean enableColors, boolean enableMiniMessage, boolean enablePlaceholders) {
         return formatLegacy(text, enableColors, enableMiniMessage, enablePlaceholders, false);
     }
     
     /**
      * Formats text with all formatting enabled (except animations) to a legacy string
      * 
      * @param text Text to format
      * @return Formatted text as a legacy string
      */
     public static String formatLegacy(String text) {
         return formatLegacy(text, true, true, true, false);
     }
 
     /**
      * Formats text for a specific player with placeholders to a legacy string
      * 
      * @param player Player for processing placeholders
      * @param text Text to format
      * @param enableColors Allow formatting with color codes and hex colors
      * @param enableMiniMessage Allow MiniMessage formatting
      * @param enablePlaceholders Allow placeholder processing
      * @param enableAnimations Allow animation processing
      * @return Formatted text as a legacy string
      */
     public static String formatLegacy(Player player, String text, boolean enableColors, boolean enableMiniMessage, boolean enablePlaceholders, boolean enableAnimations) {
         // Use the format method to get a component and then convert it to a legacy string
         Component component = format(player, text, enableColors, enableMiniMessage, enablePlaceholders, enableAnimations);
         return LEGACY_SERIALIZER.serialize(component);
     }
     
     /**
      * Formats text for a specific player (animations disabled) to a legacy string
      * 
      * @param player Player for processing placeholders
      * @param text Text to format
      * @param enableColors Allow formatting with color codes and hex colors
      * @param enableMiniMessage Allow MiniMessage formatting
      * @param enablePlaceholders Allow placeholder processing
      * @return Formatted text as a legacy string
      */
     public static String formatLegacy(Player player, String text, boolean enableColors, boolean enableMiniMessage, boolean enablePlaceholders) {
         return formatLegacy(player, text, enableColors, enableMiniMessage, enablePlaceholders, false);
     }
     
     /**
      * Formats text for a specific player with all formatting enabled (except animations) to a legacy string
      * 
      * @param player Player for processing placeholders
      * @param text Text to format
      * @return Formatted text as a legacy string
      */
     public static String formatLegacy(Player player, String text) {
         return formatLegacy(player, text, true, true, true, false);
     }
 }