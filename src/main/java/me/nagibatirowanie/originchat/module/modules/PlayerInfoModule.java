package me.nagibatirowanie.originchat.module.modules;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.module.AbstractModule;
import me.nagibatirowanie.originchat.utils.ColorUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Модуль для отображения информации о игроке при нажатии ПКМ
 */
public class PlayerInfoModule extends AbstractModule implements Listener {

    private boolean enabled;

    public PlayerInfoModule(OriginChat plugin) {
        super(plugin, "player_info", "Player Info", "Shows player information on right-click", "1.0");
    }

    @Override
    public void onEnable() {
        loadModuleConfig("modules/player_info");
        if (config == null) {
            config = plugin.getConfigManager().getMainConfig();
        }
        loadConfig();
        if (!enabled) return;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        log("PlayerInfoModule enabled");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        log("PlayerInfoModule disabled");
    }

    private void loadConfig() {
        enabled = config.getBoolean("enabled", true);
        //format = config.getString("format", "&7Player: &f{player}");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (!enabled) return;
        
        // Проверяем, что игрок кликнул по другому игроку
        if (!(event.getRightClicked() instanceof Player)) return;
        
        Player clicker = event.getPlayer();
        Player target = (Player) event.getRightClicked();
        
        // Получаем локализованный формат actionbar-сообщения
        String formattedText = getMessage(clicker, "format");
        if (formattedText == null || formattedText.startsWith("§cMessage not found")) {
            formattedText = "&7Player: &f{player}";
        }
        formattedText = formattedText.replace("{player}", target.getName());
        
        // Применяем форматирование и плейсхолдеры относительно игрока, на которого кликнули
        String finalText = ColorUtil.format(target, formattedText, true, true, true);
        
        // Отправляем actionbar игроку, который кликнул
        clicker.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(finalText));
    }
}