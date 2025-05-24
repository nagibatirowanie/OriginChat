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

 import me.clip.placeholderapi.PlaceholderAPI;
 import me.nagibatirowanie.originchat.OriginChat;
 import me.nagibatirowanie.originchat.animation.AnimationManager;
 import net.kyori.adventure.text.Component;
 import net.kyori.adventure.text.minimessage.MiniMessage;
 import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
 import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
 import net.kyori.adventure.text.format.NamedTextColor;
 
 import org.bukkit.Bukkit;
 import org.bukkit.entity.Player;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.EnumSet;
 import java.util.List;
 import java.util.Set;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 /**
  * Clean utility class for text formatting using MiniTranslator and MiniMessage
  */
 public class FormatUtil {

     private static Boolean placeholderAPIEnabled = null;
     
     // MiniMessage instance for parsing MiniMessage format
     private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
     
     // Legacy serializer for converting Component to legacy strings with colors
     private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
             .hexColors()
             .useUnusualXRepeatedCharacterHexFormat()
             .build();

     // Plain text serializer for stripping all formatting
     private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

     // MiniTranslator implementation
     private static final Set<MiniTranslatorOption> ALL_OPTIONS = Collections.unmodifiableSet(EnumSet.allOf(MiniTranslatorOption.class));
     private static final Pattern HEX_COLOR = Pattern.compile("([\\da-f]{6})");
     private static final Pattern LEGACY_HEX_COLOR = Pattern.compile("&([\\da-f])&([\\da-f])&([\\da-f])&([\\da-f])&([\\da-f])&([\\da-f])");
     
     // Pattern for detecting legacy color codes (both & and § symbols)
     private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("[&§][0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
     
     // Patterns for detecting and removing all types of color formatting
     private static final Pattern HEX_PATTERN = Pattern.compile("(?i)[&§]#[0-9a-f]{6}");
     private static final Pattern HEX_X_PATTERN = Pattern.compile("(?i)[&§]x([&§][0-9a-f]){6}");
     private static final Pattern MINIMESSAGE_COLOR_PATTERN = Pattern.compile("(?i)<(color:[^>]+|#[0-9a-f]{6}|[a-z_]+|/color|gradient:[^>]+|/gradient|rainbow[^>]*|/rainbow)>");

     /**
      * Checks if PlaceholderAPI is available
      */
     private static boolean isPlaceholderAPIEnabled() {
         if (placeholderAPIEnabled == null) {
             placeholderAPIEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
         }
         return placeholderAPIEnabled;
     }

     /**
      * Strips all color formatting from text (legacy codes, hex, MiniMessage)
      * 
      * @param text Text to strip colors from
      * @return Text without any color formatting
      */
     private static String stripAllColors(String text) {
         if (text == null || text.isEmpty()) {
             return text;
         }
         
         String result = text;
         
         // Remove legacy color codes (&c, §c, etc.)
         result = LEGACY_COLOR_PATTERN.matcher(result).replaceAll("");
         
         // Remove hex colors (&# format)
         result = HEX_PATTERN.matcher(result).replaceAll("");
         
         // Remove hex colors (&x format)
         result = HEX_X_PATTERN.matcher(result).replaceAll("");
         
         // Remove MiniMessage color tags
         result = MINIMESSAGE_COLOR_PATTERN.matcher(result).replaceAll("");
         
         return result;
     }

     /**
      * Strips legacy color codes from text
      * 
      * @param text Text to strip colors from
      * @return Text without legacy color codes
      */
     private static String stripLegacyColors(String text) {
         if (text == null || text.isEmpty()) {
             return text;
         }
         return LEGACY_COLOR_PATTERN.matcher(text).replaceAll("");
     }

     /**
      * Main formatting function - converts legacy codes to MiniMessage, then parses to Component
      * 
      * @param text Text to format
      * @param enableColors Enable color parsing (legacy codes, hex, MiniMessage)
      * @param enablePlaceholders Enable PlaceholderAPI processing
      * @param enableAnimations Enable animation processing
      * @return Formatted Component
      */
     public static Component format(String text, boolean enableColors, boolean enablePlaceholders, boolean enableAnimations) {
         if (text == null || text.isEmpty()) {
             return Component.empty();
         }

         String processed = text;

         // Process placeholders ONLY if enabled
         if (enablePlaceholders) {
             if (isPlaceholderAPIEnabled() && processed.contains("%")) {
                 try {
                     // Get any online player for placeholder processing
                     Player anyPlayer = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
                     if (anyPlayer != null) {
                         processed = PlaceholderAPI.setPlaceholders(anyPlayer, processed);
                     }
                 } catch (Exception e) {
                     Bukkit.getLogger().warning("Error processing placeholders: " + e.getMessage());
                 }
             }
         }

         // Process animations ONLY if enabled
         if (enableAnimations) {
             if (processed.contains("{animation_")) {
                 try {
                     AnimationManager animationManager = OriginChat.getInstance().getAnimationManager();
                     if (animationManager != null) {
                         // Get any online player for animation processing
                         Player anyPlayer = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
                         processed = animationManager.processAnimations(processed, anyPlayer);
                     }
                 } catch (Exception e) {
                     Bukkit.getLogger().warning("Error processing animations: " + e.getMessage());
                 }
             }
         }

         // Handle colors
         if (!enableColors) {
             // If colors are disabled, strip ALL color formatting and return plain text
             processed = stripAllColors(processed);
             return Component.text(processed);
         }

         // ВАЖНО: Преобразуем § символы в & перед обработкой MiniTranslator
         // PlaceholderAPI и другие плагины могут возвращать текст с § символами
         processed = processed.replace('§', '&');

         // Step 1: Convert legacy codes to MiniMessage format using MiniTranslator
         try {
             String miniMessageText = toMini(processed);
             
             // Step 2: Parse MiniMessage format to Component
             return MINI_MESSAGE.deserialize(miniMessageText);
         } catch (Exception e) {
             Bukkit.getLogger().warning("Error processing text formatting: " + e.getMessage());
             // Fallback: try to parse as legacy component first, then convert to plain text if that fails
             try {
                 return LEGACY_SERIALIZER.deserialize(processed);
             } catch (Exception legacyError) {
                 // Ultimate fallback: strip colors and return plain text
                 return Component.text(stripAllColors(processed));
             }
         }
     }

     /**
      * Format with player context for better placeholder processing
      * 
      * @param player Player for placeholder context
      * @param text Text to format
      * @param enableColors Enable color parsing
      * @param enablePlaceholders Enable PlaceholderAPI processing
      * @param enableAnimations Enable animation processing
      * @return Formatted Component
      */
     public static Component format(Player player, String text, boolean enableColors, boolean enablePlaceholders, boolean enableAnimations) {
         if (text == null || text.isEmpty()) {
             return Component.empty();
         }

         String processed = text;

         // Process placeholders ONLY if enabled
         if (enablePlaceholders) {
             if (player != null && isPlaceholderAPIEnabled() && processed.contains("%")) {
                 try {
                     processed = PlaceholderAPI.setPlaceholders(player, processed);
                 } catch (Exception e) {
                     Bukkit.getLogger().warning("Error processing placeholders: " + e.getMessage());
                 }
             }
         }

         // Process animations ONLY if enabled
         if (enableAnimations) {
             if (processed.contains("{animation_")) {
                 try {
                     AnimationManager animationManager = OriginChat.getInstance().getAnimationManager();
                     if (animationManager != null) {
                         processed = animationManager.processAnimations(processed, player);
                     }
                 } catch (Exception e) {
                     Bukkit.getLogger().warning("Error processing animations: " + e.getMessage());
                 }
             }
         }

         // Handle colors
         if (!enableColors) {
             // If colors are disabled, strip ALL color formatting and return plain text
             processed = stripAllColors(processed);
             return Component.text(processed);
         }

         // ВАЖНО: Преобразуем § символы в & перед обработкой MiniTranslator
         // PlaceholderAPI и другие плагины могут возвращать текст с § символами
         processed = processed.replace('§', '&');

         // Step 1: Convert legacy codes to MiniMessage format using MiniTranslator
         try {
             String miniMessageText = toMini(processed);
             
             // Step 2: Parse MiniMessage format to Component
             return MINI_MESSAGE.deserialize(miniMessageText);
         } catch (Exception e) {
             Bukkit.getLogger().warning("Error processing text formatting: " + e.getMessage());
             // Fallback: try to parse as legacy component first, then convert to plain text if that fails
             try {
                 return LEGACY_SERIALIZER.deserialize(processed);
             } catch (Exception legacyError) {
                 // Ultimate fallback: strip colors and return plain text
                 return Component.text(stripAllColors(processed));
             }
         }
     }

     // Convenience methods with default parameters

     /**
      * Format with all features enabled except animations
      */
     public static Component format(String text) {
         return format(text, true, true, false);
     }

     /**
      * Format with player context, all features enabled except animations
      */
     public static Component format(Player player, String text) {
         return format(player, text, true, true, false);
     }

     /**
      * Format with colors only (no placeholders or animations)
      */
     public static Component formatColors(String text) {
         return format(text, true, false, false);
     }

     /**
      * Format with placeholders only (no colors or animations)
      */
     public static Component formatPlaceholders(Player player, String text) {
         return format(player, text, false, true, false);
     }

     /**
      * Format plain text (no processing)
      */
     public static Component formatPlain(String text) {
         return format(text, false, false, false);
     }

     // Legacy String formatting methods

     /**
      * Format to legacy string with full control - formats to Component first, then converts to colored legacy string
      * 
      * @param text Text to format
      * @param enableColors Enable color parsing
      * @param enablePlaceholders Enable PlaceholderAPI processing
      * @param enableAnimations Enable animation processing
      * @return Formatted legacy string with colors
      */
     public static String formatLegacy(String text, boolean enableColors, boolean enablePlaceholders, boolean enableAnimations) {
         Component component = format(text, enableColors, enablePlaceholders, enableAnimations);
         return enableColors ? LEGACY_SERIALIZER.serialize(component) : PLAIN_SERIALIZER.serialize(component);
     }

     /**
      * Format to legacy string with player context - formats to Component first, then converts to colored legacy string
      * 
      * @param player Player for placeholder context
      * @param text Text to format
      * @param enableColors Enable color parsing
      * @param enablePlaceholders Enable PlaceholderAPI processing
      * @param enableAnimations Enable animation processing
      * @return Formatted legacy string with colors
      */
     public static String formatLegacy(Player player, String text, boolean enableColors, boolean enablePlaceholders, boolean enableAnimations) {
         Component component = format(player, text, enableColors, enablePlaceholders, enableAnimations);
         return enableColors ? LEGACY_SERIALIZER.serialize(component) : PLAIN_SERIALIZER.serialize(component);
     }

     // Convenience methods for legacy formatting

     /**
      * Format to legacy string with all features enabled except animations
      */
     public static String formatLegacy(String text) {
         return formatLegacy(text, true, true, false);
     }

     /**
      * Format to legacy string with player context, all features enabled except animations
      */
     public static String formatLegacy(Player player, String text) {
         return formatLegacy(player, text, true, true, false);
     }

     /**
      * Format to legacy string with colors only
      */
     public static String formatLegacyColors(String text) {
         return formatLegacy(text, true, false, false);
     }

     /**
      * Format to legacy string with placeholders only
      */
     public static String formatLegacyPlaceholders(Player player, String text) {
         return formatLegacy(player, text, false, true, false);
     }

     /**
      * Format to legacy string as plain text (no processing)
      */
     public static String formatLegacyPlain(String text) {
         return formatLegacy(text, false, false, false);
     }

     // MiniTranslator implementation methods
     
     public static String toMini(String text) {
         return toMini(text, ALL_OPTIONS);
     }

     public static String toMini(String text, MiniTranslatorOption... options) {
         return toMini(text, EnumSet.copyOf(List.of(options)));
     }

     public static String toMini(String text, Collection<MiniTranslatorOption> options) {
         List<String> closeOrder = new ArrayList<>();
         StringBuilder builder = new StringBuilder();
         for (int index = 0; index < text.length(); index++) {
             char ch = text.charAt(index);
             if (ch != '&' && ch != '§') {
                 builder.append(ch);
             } else  {
                 if (text.length() == ++index) {
                     builder.append(ch);
                     break;
                 }
                 char nextCh = text.charAt(index);
                 if (nextCh == '/') {
                     if (!options.contains(MiniTranslatorOption.END_TAGS) || text.length() == ++index) {
                         builder.append(ch).append("/");
                         continue;
                     }
                     nextCh = text.charAt(index);
                     if (nextCh == 'r' && options.contains(MiniTranslatorOption.FORMAT)) {
                         continue;
                     }
                     String tag = tagByChar(nextCh, options);
                     if (tag == null || handleClosing(tag, closeOrder)) {
                         builder.append(ch).append("/").append(nextCh);
                         continue;
                     }
                     builder.append("</").append(tag).append('>');
                 } else {
                     String tag = tagByChar(nextCh, options);
                     if (tag == null) {
                         builder.append(ch).append(nextCh);
                         continue;
                     }
                     switch (tag) {
                         case "color" -> {
                             if (nextCh == '#') {
                                 if (text.length() > index + 6) {
                                     String color = text.substring(index + 1, index + 7);
                                     if (HEX_COLOR.matcher(color).matches()) {
                                         builder.append("<color:#").append(color).append('>');
                                         index += 6;
                                         closeOrder.add(tag);
                                         continue;
                                     }
                                 }
                             } else if (nextCh == 'x') {
                                 if (text.length() > index + 12) {
                                     String color = text.substring(index + 1, index + 13);
                                     Matcher colorMatcher = LEGACY_HEX_COLOR.matcher(color);
                                     if (colorMatcher.matches()) {
                                         builder.append("<color:").append(colorMatcher.replaceAll("#$1$2$3$4$5$6")).append('>');
                                         index += 12;
                                         closeOrder.add(tag);
                                         continue;
                                     }
                                 }
                             }
                             builder.append(ch).append(nextCh);
                         }
                         case "gradient" -> {
                             int endIndex = -1;
                             for (int inner = index + 1; inner < text.length(); inner++) {
                                 char inCh = text.charAt(inner);
                                 if (inCh == '&' || inCh == '§') {
                                     endIndex = inner;
                                     break;
                                 } else if (!(('a' <= inCh && inCh <= 'z') || ('0' <= inCh && inCh <= '9') || inCh == '#' || inCh == '-')) {
                                     break;
                                 }
                             }
                             String[] split;
                             if (endIndex == -1 || (split = text.substring(index + 1, endIndex).split("-")).length == 1) {
                                 builder.append(ch).append("@");
                                 continue;
                             }
                             List<String> colors = new ArrayList<>(split.length);
                             for (String color : split) {
                                 if (color.length() == 1) {
                                     color = colorByChar(color.charAt(0));
                                     if (color == null) break;
                                 } else if (color.startsWith("#")) {
                                     if (!HEX_COLOR.matcher(color.substring(1)).matches()) {
                                         break;
                                     }
                                 } else if (NamedTextColor.NAMES.value(color) == null) {
                                     break;
                                 }
                                 colors.add(color);
                             }
                             if (colors.size() == split.length) {
                                 index = endIndex;
                                 builder.append("<gradient:").append(String.join(":", colors)).append('>');
                                 closeOrder.add(tag);
                             }
                         }
                         case "reset" -> {
                             closeOrder.clear();
                             builder.append("<reset>");
                         }
                         default -> {
                             closeOrder.add(tag);
                             builder.append('<').append(tag).append('>');
                         }
                     }
                 }
             }
         }
         return builder.toString();
     }

     private static boolean handleClosing(String tag, List<String> order) {
         int index = order.lastIndexOf(tag);
         if (index == -1) {
             return true;
         }
         order.subList(index, order.size()).clear();
         return false;
     }

     private static String colorByChar(char ch) {
         return switch (ch) {
             case '0' -> "black";
             case '1' -> "dark_blue";
             case '2' -> "dark_green";
             case '3' -> "dark_aqua";
             case '4' -> "dark_red";
             case '5' -> "dark_purple";
             case '6' -> "gold";
             case '7' -> "gray";
             case '8' -> "dark_gray";
             case '9' -> "blue";
             case 'a' -> "green";
             case 'b' -> "aqua";
             case 'c' -> "red";
             case 'd' -> "light_purple";
             case 'e' -> "yellow";
             case 'f' -> "white";

             default -> null;
         };
     }

     public static String tagByChar(char ch, Collection<MiniTranslatorOption> options) {
         if (('0' <= ch && ch <= '9') || ('a' <= ch && ch <= 'f') || ch == '#' || ch == 'x') {
             if (!options.contains(MiniTranslatorOption.COLOR)) return null;
             return switch (ch) {
                 case 'x', '#' -> "color";
                 default -> colorByChar(ch);
             };
         } else if (('k' <= ch && ch <= 'o') || ch == 'r') {
             if (!options.contains(MiniTranslatorOption.FORMAT)) return null;
             return switch (ch) {
                 case 'r' -> "reset";
                 case 'l' -> "b";
                 case 'n' -> "u";
                 case 'm' -> "st";
                 case 'o' -> "i";
                 case 'k' -> "obf";

                 default -> null;
             };
         } else if (ch == '@' && options.contains(MiniTranslatorOption.GRADIENT)) {
             return "gradient";
         }
         return null;
     }

     public enum MiniTranslatorOption {
         COLOR, FORMAT, GRADIENT, END_TAGS
     }
 }