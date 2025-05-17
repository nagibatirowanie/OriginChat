package me.nagibatirowanie.originchat.module.modules;

import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.module.AbstractModule;
import me.nagibatirowanie.originchat.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Module that displays information about a player when right-clicked.
 */
public class PlayerInfoModule extends AbstractModule implements Listener {

    private boolean enabled;

    public PlayerInfoModule(OriginChat plugin) {
        super(plugin, "player_info", "Player Info", 
              "Shows player information on right-click", "1.0");
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

    /**
     * Loads configuration settings for this module.
     */
    private void loadConfig() {
        enabled = config.getBoolean("enabled", true);
    }

    /**
     * Handles right-click interactions on players to show their information.
     *
     * @param event the player interact entity event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (!enabled) return;

        if (!(event.getRightClicked() instanceof Player)) return;

        Player clicker = event.getPlayer();
        Player target = (Player) event.getRightClicked();

        String template = getMessage(clicker, "format");
        if (template == null || template.startsWith("§cMessage not found")) {
            template = "&7Player: &f{player}";
        }
        String replaced = template.replace("{player}", target.getName());
        String formatted = ColorUtil.format(target, replaced, true, true, true);

        Component component = LegacyComponentSerializer.legacySection().deserialize(formatted);
        clicker.sendActionBar(component);
    }
}