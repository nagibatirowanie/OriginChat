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

import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4;

/**
 * Утилитный класс для перевода текста с использованием Google Translate API
 */
public class TranslateUtil {
    
    private static final Pattern TRANSLATION_PATTERN = Pattern.compile("class=\"result-container\">([^<]*)<\\/div>", Pattern.MULTILINE);
    
    /**
     * Нормализует код языка для использования с Google Translate
     * 
     * @param langCode Код языка (например, "en", "ru", "uk", "uk_UA")
     * @return Нормализованный код языка для Google Translate
     */
    private static String normalizeLanguageCode(String langCode) {
        if (langCode == null || langCode.isEmpty()) {
            return "en"; // Возвращаем английский по умолчанию
        }
        
        // Заменяем подчеркивание на дефис для совместимости с Google Translate
        if (langCode.contains("_")) {
            String[] parts = langCode.split("_");
            // Для большинства языков достаточно только кода языка
            // Но для некоторых (китайский, японский и т.д.) важен и региональный код
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
     * Переводит текст на указанный язык
     *
     * @param text    Исходный текст для перевода
     * @param toLang  Целевой язык (код языка, например "en", "ru", "uk", "uk-UA")
     * @return        Переведенный текст
     * @throws IOException в случае ошибки сети или перевода
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
            String errorMsg = String.format("Ошибка при обращении к сервису перевода. Язык: %s, Текст: '%s', URL: %s, Сообщение: %s", normalizedLang, text, url, e.getMessage());
            System.err.println("[TranslateUtil] " + errorMsg);
            throw new IOException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format("Неизвестная ошибка при переводе. Язык: %s, Текст: '%s', URL: %s, Сообщение: %s", normalizedLang, text, url, e.getMessage());
            System.err.println("[TranslateUtil] " + errorMsg);
            throw new IOException(errorMsg, e);
        }
        Matcher matcher = TRANSLATION_PATTERN.matcher(response);
        if (matcher.find()) {
            String match = matcher.group(1);
            if (match != null && !match.isEmpty()) {
                return unescapeHtml4(match);
            }
        }
        String errorMsg = String.format("Не удалось выполнить перевод. Язык: %s, Текст: '%s', URL: %s, Ответ: %s", normalizedLang, text, url, response.toString());
        System.err.println("[TranslateUtil] " + errorMsg);
        throw new IOException(errorMsg);
    }

    /**
     * Асинхронно переводит текст на указанный язык
     *
     * @param text    Исходный текст для перевода
     * @param toLang  Целевой язык (код языка, например "en", "ru", "uk", "uk-UA", "uk_UA")
     * @return        CompletableFuture с результатом перевода
     */
    public static CompletableFuture<String> translateAsync(String text, String toLang) {
        System.out.println("[TranslateUtil] Запрос на перевод текста на язык: " + toLang);
        return CompletableFuture.supplyAsync(() -> {
            try {
                String result = translate(text, toLang);
                System.out.println("[TranslateUtil] Успешный перевод на язык: " + toLang);
                return result;
            } catch (IOException e) {
                String errorMsg = String.format("Ошибка перевода на язык %s. Текст: '%s'. Причина: %s", toLang, text, e.getMessage());
                System.err.println("[TranslateUtil] " + errorMsg);
                throw new RuntimeException(errorMsg, e);
            } catch (Exception e) {
                String errorMsg = String.format("Неизвестная ошибка перевода на язык %s. Текст: '%s'. Причина: %s", toLang, text, e.getMessage());
                System.err.println("[TranslateUtil] " + errorMsg);
                throw new RuntimeException(errorMsg, e);
            }
        });
    }

    /**
     * Определяет язык переданного текста
     *
     * @param text Текст для определения языка
     * @return Предполагаемый язык текста (английское описание)
     */
    public static String detectLanguage(String text) {
        try {
            // Создаем специальный URL для определения языка
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
            
            // Ищем информацию о языке в ответе
            Pattern languagePattern = Pattern.compile("class=\"[^\"]*?\">\\s*Translated from\\s+([^<]+)\\s*<", Pattern.MULTILINE);
            Matcher matcher = languagePattern.matcher(response);
            
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
            
            return "Неизвестный";
        } catch (Exception e) {
            return "Неизвестный";
        }
    }
}

class TranslateExample {
    
    public static void main(String[] args) {
        // Пример 2: Асинхронный перевод
        TranslateUtil.translateAsync("Hello world", "ru")
            .thenAccept(translation -> System.out.println("Асинхронный перевод: " + translation))
            .exceptionally(ex -> {
                System.err.println("Ошибка асинхронного перевода: " + ex.getMessage());
                return null;
            });
        
    }
}