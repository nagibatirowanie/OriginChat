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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.nagibatirowanie.originchat.OriginChat;
import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4;

/**
 * Utility class for text translation using Google Translate API
 */
public class TranslateUtil {
    
    private static final Pattern TRANSLATION_PATTERN = Pattern.compile("class=\"result-container\">([^<]*)<\\/div>", Pattern.MULTILINE);
    private static final OriginChat plugin = OriginChat.getInstance();
    
    /**
     * Normalizes language code for use with Google Translate
     * 
     * @param langCode Language code (e.g., "en", "ru", "uk", "uk_UA")
     * @return Normalized language code for Google Translate
     */
    private static String normalizeLanguageCode(String langCode) {
        if (langCode == null || langCode.isEmpty()) {
            return "en"; // Return English by default
        }
        
        // Replace underscore with hyphen for compatibility with Google Translate
        if (langCode.contains("_")) {
            String[] parts = langCode.split("_");
            // For some languages (Chinese, Japanese, etc.) regional code is important
            if (parts[0].equals("zh") || parts[0].equals("ja") || 
                parts[0].equals("ko") || parts[0].equals("pt") ||
                parts[0].equals("uk") || parts[0].equals("ru") ||
                parts[0].equals("be") || parts[0].equals("kk") ||
                parts[0].equals("uz") || parts[0].equals("ky")) {
                return parts[0].toLowerCase() + "-" + parts[1].toUpperCase();
            } else {
                return parts[0].toLowerCase();
            }
        }
        
        return langCode.toLowerCase();
    }

    /**
     * Translates text to the specified language
     *
     * @param text    Source text to translate
     * @param toLang  Target language (language code, e.g., "en", "ru", "uk", "uk-UA")
     * @return        Translated text
     * @throws IOException in case of network or translation error
     */
    public static String translate(String text, String toLang) throws IOException {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        String normalizedLang = normalizeLanguageCode(toLang);
        StringBuilder response = new StringBuilder();
        String encodedText = URLEncoder.encode(text.trim(), StandardCharsets.UTF_8);
        URL url = new URL(String.format("https://translate.google.com/m?hl=en&sl=auto&tl=%s&ie=UTF-8&prev=_m&q=%s", 
                normalizedLang, encodedText));
        try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line).append("\n");
            }
        } catch (IOException e) {
            String errorMsg = String.format("Error accessing translation service. Language: %s, Text: '%s', URL: %s, Message: %s", 
                    normalizedLang, text, url, e.getMessage());
            plugin.getLogger().warning(errorMsg);
            throw new IOException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format("Unknown error during translation. Language: %s, Text: '%s', URL: %s, Message: %s", 
                    normalizedLang, text, url, e.getMessage());
            plugin.getLogger().warning(errorMsg);
            throw new IOException(errorMsg, e);
        }
        Matcher matcher = TRANSLATION_PATTERN.matcher(response);
        if (matcher.find()) {
            String match = matcher.group(1);
            if (match != null && !match.isEmpty()) {
                return unescapeHtml4(match);
            }
        }
        String errorMsg = String.format("Failed to perform translation. Language: %s, Text: '%s', URL: %s", 
                normalizedLang, text, url);
        plugin.getLogger().warning(errorMsg);
        throw new IOException(errorMsg);
    }

    /**
     * Asynchronously translates text to the specified language
     *
     * @param text    Source text to translate
     * @param toLang  Target language (language code, e.g., "en", "ru", "uk", "uk-UA", "uk_UA")
     * @return        CompletableFuture with translation result
     */
    public static CompletableFuture<String> translateAsync(String text, String toLang) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return translate(text, toLang);
            } catch (IOException e) {
                String errorMsg = String.format("Translation error to language %s. Text: '%s'. Reason: %s", 
                        toLang, text, e.getMessage());
                plugin.getLogger().warning(errorMsg);
                throw new RuntimeException(errorMsg, e);
            } catch (Exception e) {
                String errorMsg = String.format("Unknown translation error to language %s. Text: '%s'. Reason: %s", 
                        toLang, text, e.getMessage());
                plugin.getLogger().warning(errorMsg);
                throw new RuntimeException(errorMsg, e);
            }
        });
    }

    /**
     * Detects the language of provided text
     *
     * @param text Text for language detection
     * @return Presumed language of the text (English description)
     */
    public static String detectLanguage(String text) {
        try {
            // Create special URL for language detection
            String encodedText = URLEncoder.encode(text.trim(), StandardCharsets.UTF_8);
            URL url = new URL(String.format("https://translate.google.com/m?hl=en&sl=auto&tl=en&ie=UTF-8&prev=_m&q=%s", 
                    encodedText));
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line).append("\n");
                }
            }
            
            // Find language information in the response
            Pattern languagePattern = Pattern.compile("class=\"[^\"]*?\">\\s*Translated from\\s+([^<]+)\\s*<", Pattern.MULTILINE);
            Matcher matcher = languagePattern.matcher(response);
            
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
            
            return "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }
}