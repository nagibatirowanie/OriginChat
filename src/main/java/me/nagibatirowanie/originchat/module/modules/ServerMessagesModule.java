package me.nagibatirowanie.originchat.module.modules;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.module.AbstractModule;
import me.nagibatirowanie.originchat.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Random;

/**
 * Модуль для управления сообщениями входа/выхода игроков
 */
public class ServerMessagesModule extends AbstractModule implements Listener {

    private boolean joinMessageEnabled;
    private boolean leaveMessageEnabled;
    private boolean personalWelcomeEnabled;
    private List<String> joinMessages;
    private List<String> leaveMessages;
    private List<String> personalWelcomeMessages;

    public ServerMessagesModule(OriginChat plugin) {
        super(plugin, "servermessages", "Серверные сообщения", "Управление сообщениями входа/выхода игроков", "1.0");
    }

    @Override
    public void onEnable() {
        loadModuleConfig("modules/servermessages");
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        log("Модуль серверных сообщений успешно загружен.");
    }

    @Override
    public void onDisable() {
        PlayerJoinEvent.getHandlerList().unregister(this);
        PlayerQuitEvent.getHandlerList().unregister(this);
        log("Модуль серверных сообщений выключен.");
    }

    /**
     * Загрузить настройки из конфигурации
     */
    protected void loadConfig() {
        try {
            joinMessageEnabled = config.getBoolean("join_message_enabled", true);
            leaveMessageEnabled = config.getBoolean("leave_message_enabled", true);
            personalWelcomeEnabled = config.getBoolean("personal_welcome_enabled", true);
            joinMessages = config.getStringList("join_messages");
            leaveMessages = config.getStringList("leave_messages");
            personalWelcomeMessages = config.getStringList("personal_welcome_messages");
            
            // Проверка на пустые списки и добавление стандартных сообщений
            if (joinMessages.isEmpty()) {
                joinMessages.add("&a+ &f{player} &7присоединился к серверу");
                config.set("join_messages", joinMessages);
            }
            
            if (leaveMessages.isEmpty()) {
                leaveMessages.add("&c- &f{player} &7покинул сервер");
                config.set("leave_messages", leaveMessages);
            }
            
            if (personalWelcomeMessages.isEmpty()) {
                personalWelcomeMessages.add("&6Добро пожаловать на сервер, &f{player}&6!");
                config.set("personal_welcome_messages", personalWelcomeMessages);
            }
            
            saveModuleConfig("modules/servermessages");
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Ошибка при загрузке конфигурации ServerMessagesModule: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (personalWelcomeEnabled && !personalWelcomeMessages.isEmpty()) {
            String welcomeMessage = getRandomMessage(personalWelcomeMessages, player);
            player.sendMessage(ColorUtil.format(player, welcomeMessage));
        }

        if (joinMessageEnabled && !joinMessages.isEmpty()) {
            String joinMessage = getRandomMessage(joinMessages, player);
            event.setJoinMessage(ColorUtil.format(player, joinMessage));
        } else {
            event.setJoinMessage(null);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (leaveMessageEnabled && !leaveMessages.isEmpty()) {
            String leaveMessage = getRandomMessage(leaveMessages, player);
            event.setQuitMessage(ColorUtil.format(player, leaveMessage));
        } else {
            event.setQuitMessage(null);
        }
    }

    /**
     * Получить случайное сообщение из списка и заменить плейсхолдеры
     * @param messages список сообщений
     * @param player игрок для замены плейсхолдеров
     * @return случайное сообщение с замененными плейсхолдерами
     */
    private String getRandomMessage(List<String> messages, Player player) {
        String message = messages.get(new Random().nextInt(messages.size()));
        return message.replace("{player}", player.getName());
    }
    
    /**
     * Получить случайное сообщение из списка
     * @param messages список сообщений
     * @return случайное сообщение
     */
    private String getRandomMessage(List<String> messages) {
        return messages.get(new Random().nextInt(messages.size()));
    }
}