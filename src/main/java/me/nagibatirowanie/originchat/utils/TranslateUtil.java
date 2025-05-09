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
     * Переводит текст на указанный язык
     *
     * @param text    Исходный текст для перевода
     * @param toLang  Целевой язык (код языка, например "en", "ru", "uk")
     * @return        Переведенный текст
     * @throws IOException в случае ошибки сети или перевода
     */
    public static String translate(String text, String toLang) throws IOException {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        StringBuilder response = new StringBuilder();
        String encodedText = URLEncoder.encode(text.trim(), StandardCharsets.UTF_8);
        URL url = new URL(String.format("https://translate.google.com/m?hl=en&sl=auto&tl=%s&ie=UTF-8&prev=_m&q=%s", 
                toLang, encodedText));

        try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line).append("\n");
            }
        }

        Matcher matcher = TRANSLATION_PATTERN.matcher(response);
        if (matcher.find()) {
            String match = matcher.group(1);
            if (match != null && !match.isEmpty()) {
                return unescapeHtml4(match);
            }
        }
        
        throw new IOException("Не удалось выполнить перевод");
    }

    /**
     * Асинхронно переводит текст на указанный язык
     *
     * @param text    Исходный текст для перевода
     * @param toLang  Целевой язык (код языка, например "en", "ru", "uk")
     * @return        CompletableFuture с результатом перевода
     */
    public static CompletableFuture<String> translateAsync(String text, String toLang) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return translate(text, toLang);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка перевода: " + e.getMessage(), e);
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