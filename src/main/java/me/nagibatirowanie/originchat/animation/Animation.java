package me.nagibatirowanie.originchat.animation;

import me.nagibatirowanie.originchat.OriginChat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class representing an animation with a set of frames.
 * Supports localization of frames for different languages.
 */
public class Animation {

    private final String name;
    private final int interval; // Interval in ticks
    private final List<String> frames; // Default frames (for backward compatibility)
    private final Map<String, List<String>> localizedFrames; // Frames for different languages
    private int currentFrameIndex;
    private int tickCounter;

    // Constant for the default language
    public static final String DEFAULT_LOCALE = getDefaultLocale();

    /**
     * Creates a new animation
     * @param name animation name
     * @param interval frame interval in ticks
     * @param frames list of frames (for backward compatibility)
     */
    public Animation(String name, int interval, List<String> frames) {
        this.name = name;
        this.interval = Math.max(1, interval); // Minimum interval is 1 tick
        this.frames = frames;
        this.localizedFrames = new HashMap<>();
        this.localizedFrames.put(DEFAULT_LOCALE, frames); // Add default frames as default locale
        this.currentFrameIndex = 0;
        this.tickCounter = 0;
    }

    /**
     * Creates a new animation with localization support
     * @param name animation name
     * @param interval frame interval in ticks
     * @param localizedFrames map of localized frames (key - locale code, value - list of frames)
     */
    public Animation(String name, int interval, Map<String, List<String>> localizedFrames) {
        this.name = name;
        this.interval = Math.max(1, interval); // Minimum interval is 1 tick
        this.localizedFrames = localizedFrames;

        // For backward compatibility, set frames to the default locale frames
        this.frames = localizedFrames.getOrDefault(DEFAULT_LOCALE, List.of());

        this.currentFrameIndex = 0;
        this.tickCounter = 0;
    }

    /**
     * Updates tick counter and switches frame if needed
     */
    public void nextFrame() {
        tickCounter = (tickCounter + 1) % Integer.MAX_VALUE;

        if (tickCounter >= interval) {
            tickCounter = 0;
            if (!frames.isEmpty()) {
                currentFrameIndex = (currentFrameIndex + 1) % frames.size();
            }
        }
    }

    /**
     * Gets the current animation frame for the specified locale
     * @param locale locale code (e.g., "ru", "en")
     * @return current frame for the specified locale, or for default locale if translation not found
     */
    public String getCurrentFrame(String locale) {
        boolean autoDetect = OriginChat.getInstance().getConfig().getBoolean("locale.auto_detect", true);

        if (!autoDetect) {
            locale = getDefaultLocale();
        }

        List<String> framesForLocale = getFramesForLocale(locale);

        if (framesForLocale == null || framesForLocale.isEmpty()) {
            return "";
        }

        if (currentFrameIndex < 0 || currentFrameIndex >= framesForLocale.size()) {
            currentFrameIndex = 0;
            return framesForLocale.get(0);
        }

        return framesForLocale.get(currentFrameIndex);
    }

    /**
     * Gets the current animation frame for the default locale
     * @return current frame
     */
    public String getCurrentFrame() {
        return getCurrentFrame(DEFAULT_LOCALE);
    }

    /**
     * Gets the animation name
     * @return animation name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the frame interval
     * @return interval in ticks
     */
    public int getInterval() {
        return interval;
    }

    /**
     * Gets all frames for the specified locale
     * @param locale locale code
     * @return list of frames for the locale, or for default locale if not found
     */
    public List<String> getFramesForLocale(String locale) {
        boolean autoDetect = OriginChat.getInstance().getConfig().getBoolean("locale.auto_detect", true);

        if (!autoDetect) {
            locale = getDefaultLocale();
        }

        return localizedFrames.getOrDefault(locale, localizedFrames.getOrDefault(getDefaultLocale(), List.of()));
    }

    /**
     * Gets all frames for the default locale
     * @return list of frames
     */
    public List<String> getFrames() {
        return getFramesForLocale(DEFAULT_LOCALE);
    }

    /**
     * Gets the map of all localized frames
     * @return map of localized frames
     */
    public Map<String, List<String>> getLocalizedFrames() {
        return localizedFrames;
    }

    /**
     * Checks if frames exist for the specified locale
     * @param locale locale code
     * @return true if frames exist for the locale
     */
    public boolean hasLocale(String locale) {
        return localizedFrames.containsKey(locale) && !localizedFrames.get(locale).isEmpty();
    }

    /**
     * Gets the default locale from plugin config
     * @return default locale code
     */
    public static String getDefaultLocale() {
        try {
            return OriginChat.getInstance().getConfig().getString("locale.default", "ru");
        } catch (Exception e) {
            return "ru"; // Return "ru" in case of error
        }
    }

    /**
     * Gets the list of available locales for this animation
     * @return list of locale codes
     */
    public List<String> getAvailableLocales() {
        return List.copyOf(localizedFrames.keySet());
    }

    /**
     * Gets the index of the current frame
     * @return index of the current frame
     */
    public int getCurrentFrameIndex() {
        return currentFrameIndex;
    }

    /**
     * Sets a specific frame index as the current frame
     * @param index frame index
     */
    public void setCurrentFrameIndex(int index) {
        if (frames.isEmpty()) return;
        this.currentFrameIndex = Math.max(0, Math.min(index, frames.size() - 1));
        this.tickCounter = 0;
    }
}
