package me.nagibatirowanie.originchat.module.modules;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.injector.netty.WirePacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.nagibatirowanie.originchat.OriginChat;
import me.nagibatirowanie.originchat.module.AbstractModule;
import me.nagibatirowanie.originchat.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.EventHandler;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ServerBrandModule extends AbstractModule implements Listener {
    // Конфигурируемые поля
    private List<String> serverBrands;
    private long updatePeriod;
    private boolean enabled;

    // ProtocolLib
    private ProtocolManager protocolManager;

    // Для цикличного обновления
    private ScheduledFuture<?> updateTask;
    private int currentIndex = 0;

    // Название канала Custom Payload
    private static final String CHANNEL = "minecraft:brand";

    public ServerBrandModule(OriginChat plugin) {
        super(plugin, "server_brand", "Server Brand",
              "Изменяет название ядра сервера в F3 меню", "2.0");
    }

    @Override
    public void onEnable() {
        // 1) Загрузка конфига модуля
        loadModuleConfig("modules/server_brand");
        if (config == null) {
            log("Не удалось загрузить конфигурацию модуля ServerBrand");
            return;
        }
        loadConfigValues();

        if (!enabled) {
            log("Модуль ServerBrand отключен в конфигурации");
            return;
        }

        // 2) Инициализация ProtocolLib
        try {
            protocolManager = ProtocolLibrary.getProtocolManager();
            if (protocolManager == null) {
                log("❗ ProtocolLib не найден. ServerBrand отключён.");
                enabled = false;
                return;
            }
        } catch (Exception e) {
            log("❗ Ошибка при инициализации ProtocolLib: " + e.getMessage());
            enabled = false;
            return;
        }

        // 3) Регистрируем слушатель для новых заходов
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // 4) Моментальная рассылка всем онлайн
        broadcastBrand();

        // 5) Запускаем задачу цикличного обновления
        if (serverBrands.size() > 1) {
            updateTask = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                this::broadcastBrand,
                updatePeriod,
                updatePeriod,
                TimeUnit.MILLISECONDS
            );
            log("Запущено обновление бренда каждые " + updatePeriod + "мс");
        }

        log("Модуль ServerBrand успешно включен");
    }

    @Override
    public void onDisable() {
        if (updateTask != null) {
            updateTask.cancel(true);
            updateTask = null;
        }
        log("Модуль ServerBrand отключен");
    }

    /** Загружает значения enabled, serverBrands и updatePeriod из конфига */
    private void loadConfigValues() {
        enabled = config.getBoolean("enabled", true);

        // Поддержка старого и нового форматов
        serverBrands = new ArrayList<>();
        if (config.contains("server_brand")) {
            serverBrands.add(config.getString("server_brand", "&bOriginChat"));
        } else if (config.contains("server_brands")) {
            serverBrands.addAll(config.getStringList("server_brands"));
        }
        if (serverBrands.isEmpty()) {
            serverBrands.add("&bOriginChat");
        }

        updatePeriod = config.getLong("update_period", 5000L);
        log("Конфиг: enabled=" + enabled +
            ", brands=" + serverBrands +
            ", updatePeriod=" + updatePeriod + "мс");
    }

    /** При входе нового игрока досылаем текущий бренд */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        Player p = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> sendBrand(p));
    }

    /** Рассылает текущий бренд всем онлайн и инкрементит index */
    private void broadcastBrand() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendBrand(p);
        }
        currentIndex = (currentIndex + 1) % serverBrands.size();
    }

    /** Формирует и отправляет WirePacket для замены F3-бренда конкретному игроку */
    private void sendBrand(Player player) {
        if (!player.isOnline() || !enabled) return;

        try {
            // 1) Берём строку, подставляем {name}/{displayname}, красим
            String raw = serverBrands.get(currentIndex)
                .replace("{name}", player.getName())
                .replace("{displayname}", player.getDisplayName());
            String formatted = ColorUtil.format(raw, true, true, true) + "§r";

            // 2) Пишем в ByteBuf: сначала канал, затем текст
            ByteBuf buf = Unpooled.buffer();
            writeString(buf, CHANNEL);
            writeString(buf, formatted);

            // 3) Копируем данные
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);

            // 4) Шлём пакет
            WirePacket wp = new WirePacket(PacketType.Play.Server.CUSTOM_PAYLOAD, data);
            protocolManager.sendWirePacket(player, wp);

            debug("Sent brand to " + player.getName() + ": " + formatted);
        } catch (Throwable t) {
            log("Ошибка при отправке бренда игроку " + player.getName() + ": " + t.getMessage());
            if (config.getBoolean("debug", false)) t.printStackTrace();
        }
    }

    /** Записывает Minecraft-строку (VarInt длина + UTF-8) */
    private void writeString(ByteBuf buf, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    /** Стандартный алгоритм VarInt для протокола */
    private void writeVarInt(ByteBuf buf, int value) {
        while ((value & 0xFFFFFF80) != 0) {
            buf.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buf.writeByte(value & 0x7F);
    }
}
