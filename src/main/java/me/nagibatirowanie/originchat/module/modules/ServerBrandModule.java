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

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.injector.netty.WirePacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.module.AbstractModule;
import me.nagibatirowanie.originchat.utils.FormatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Module for broadcasting custom server brand names in the F3 menu using ProtocolLib.
 */
public class ServerBrandModule extends AbstractModule implements Listener {

    private static final String CHANNEL = "minecraft:brand";
    private final List<String> serverBrands = new ArrayList<>();
    private long updatePeriod;
    private boolean enabled;
    private ProtocolManager protocolManager;
    private ScheduledFuture<?> updateTask;
    private int currentIndex = 0;

    /**
     * Constructs the ServerBrandModule.
     *
     * @param plugin the main OriginChat plugin instance
     */
    public ServerBrandModule(OriginChat plugin) {
        super(plugin, "server_brand", "Server Brand",
              "Broadcasts custom server brands via custom payload packets", "2.0");
    }

    /**
     * Called when the module is enabled.
     * Loads configuration, initializes ProtocolLib, registers events, broadcasts the brand,
     * and schedules periodic updates if multiple brands are configured.
     */
    @Override
    public void onEnable() {
        loadModuleConfig("modules/server_brand");
        if (config == null) {
            log("Failed to load ServerBrand configuration.");
            return;
        }
        loadConfigValues();
        if (!enabled) {
            log("ServerBrand module is disabled in configuration.");
            return;
        }

        try {
            protocolManager = ProtocolLibrary.getProtocolManager();
            if (protocolManager == null) {
                log("ProtocolLib not found, disabling ServerBrand module.");
                enabled = false;
                return;
            }
        } catch (Exception e) {
            log("Error initializing ProtocolLib: " + e.getMessage());
            enabled = false;
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
        broadcastBrand();

        if (serverBrands.size() > 1) {
            updateTask = Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(this::broadcastBrand, updatePeriod, updatePeriod, TimeUnit.MILLISECONDS);
            log("Scheduled brand update every " + updatePeriod + " ms.");
        }

        log("ServerBrand module enabled successfully.");
    }

    /**
     * Called when the module is disabled.
     * Cancels any scheduled tasks.
     */
    @Override
    public void onDisable() {
        if (updateTask != null) {
            updateTask.cancel(true);
            updateTask = null;
        }
        log("ServerBrand module disabled.");
    }

    /**
     * Loads the 'enabled', 'serverBrands', and 'updatePeriod' values from the module config.
     */
    private void loadConfigValues() {
        enabled = config.getBoolean("enabled", true);

        if (config.contains("server_brand")) {
            serverBrands.add(config.getString("server_brand", "&bOriginChat"));
        } else if (config.contains("server_brands")) {
            serverBrands.addAll(config.getStringList("server_brands"));
        }
        if (serverBrands.isEmpty()) {
            serverBrands.add("&bOriginChat");
        }

        updatePeriod = config.getLong("update_period", 5000L);
        log("Config values: enabled=" + enabled
            + ", brands=" + serverBrands
            + ", updatePeriod=" + updatePeriod + " ms");
    }

    /**
     * Sends the current brand to all online players and advances the index.
     */
    private void broadcastBrand() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendBrand(player);
        }
        currentIndex = (currentIndex + 1) % serverBrands.size();
    }

    /**
     * Sends the current brand packet to a single player.
     *
     * @param player the player to receive the brand packet
     */
    private void sendBrand(Player player) {
        if (!player.isOnline() || !enabled) {
            return;
        }
        try {
            String raw = serverBrands.get(currentIndex)
                .replace("{name}", player.getName())
                .replace("{displayname}", player.getDisplayName());
            String formatted = FormatUtil.formatLegacy(raw) + "§r";

            ByteBuf buffer = Unpooled.buffer();
            writeString(buffer, CHANNEL);
            writeString(buffer, formatted);

            byte[] data = new byte[buffer.readableBytes()];
            buffer.readBytes(data);

            WirePacket packet = new WirePacket(PacketType.Play.Server.CUSTOM_PAYLOAD, data);
            protocolManager.sendWirePacket(player, packet);

            debug("Sent brand to " + player.getName() + ": " + formatted);
        } catch (Throwable t) {
            log("Error sending brand to " + player.getName() + ": " + t.getMessage());
            if (config.getBoolean("debug", false)) {
                t.printStackTrace();
            }
        }
    }

    /**
     * Writes a Minecraft string to the buffer (VarInt length + UTF-8 bytes).
     *
     * @param buf the target ByteBuf
     * @param s   the string to write
     */
    private void writeString(ByteBuf buf, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    /**
     * Writes a VarInt to the buffer according to the Minecraft protocol.
     *
     * @param buf   the target ByteBuf
     * @param value the integer value to write
     */
    private void writeVarInt(ByteBuf buf, int value) {
        while ((value & 0xFFFFFF80) != 0) {
            buf.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buf.writeByte(value & 0x7F);
    }

    /**
     * Sends the current brand to a player when they join.
     *
     * @param event the PlayerJoinEvent triggered when a player joins
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) {
            return;
        }
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> sendBrand(player));
    }
}
