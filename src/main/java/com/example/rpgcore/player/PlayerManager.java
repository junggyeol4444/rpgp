package com.example.rpgcore.player;

import com.example.rpgcore.core.Lifecycle;
import com.example.rpgcore.player.data.PlayerData;
import com.example.rpgcore.storage.cache.PlayerDataCache;
import com.example.rpgcore.storage.dirty.SavePriority;
import com.example.rpgcore.storage.dirty.SaveScheduler;
import com.example.rpgcore.util.Messages;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 지시서 3장 [player/PlayerManager] — 접속·퇴장 처리.
 *
 * <p>지시서 5장 [YAML 구현]:
 * 접속 시 비동기로 읽고, 완료된 뒤 메인 스레드에서 캐시에 올린다.
 * 퇴장 시 저장하고 캐시에서 내린다.
 *
 * <p>로드가 끝나기 전에 나가버린 경우에는 캐시에 올리지 않는다.
 */
public final class PlayerManager implements Lifecycle, Listener {

    private final PlayerDataCache cache;
    private final SaveScheduler saves;
    private final Messages messages;
    private final Logger logger;

    private final Map<UUID, RpgPlayer> online = new ConcurrentHashMap<>();

    public PlayerManager(PlayerDataCache cache, SaveScheduler saves,
                         Messages messages, Logger logger) {
        this.cache = cache;
        this.saves = saves;
        this.messages = messages;
        this.logger = logger;
    }

    @Override
    public String serviceName() {
        return "PlayerManager";
    }

    /**
     * 이미 접속해 있는 플레이어의 데이터를 읽어 온다.
     *
     * <p>서버가 도는 중에 플러그인을 다시 켠 경우 PlayerJoinEvent 가
     * 오지 않으므로, 켜질 때 한 번 훑어준다.
     */
    public void loadOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadFor(player);
        }
    }

    @Override
    public void disable() {
        // 저장은 SaveScheduler.disable() 이 맡는다. 여기서는 참조만 정리한다.
        online.clear();
    }

    /** 접속 중이면 런타임 상태, 아직 로드 중이거나 나갔으면 null. */
    public RpgPlayer get(Player player) {
        return online.get(player.getUniqueId());
    }

    public RpgPlayer get(UUID uuid) {
        return online.get(uuid);
    }

    public Collection<RpgPlayer> onlinePlayers() {
        return online.values();
    }

    public int loadedCount() {
        return online.size();
    }

    /** 문구 출력용. */
    public Messages messages() {
        return messages;
    }

    /**
     * 플레이어 데이터를 기본값으로 되돌린다. (/rpg admin datareset)
     *
     * <p>기존 객체를 고치지 않고 새 데이터로 갈아끼운다. 스키마가 늘어나도
     * 초기화 로직을 따로 손볼 필요가 없다.
     */
    public void resetData(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData fresh = new PlayerData(uuid);
        fresh.name(player.getName());
        fresh.lastLogin(System.currentTimeMillis());
        cache.put(fresh);
        online.put(uuid, new RpgPlayer(player, fresh));
        // 되돌릴 수 없는 조작이므로 즉시 저장한다.
        saves.markDirty(fresh, SavePriority.IMMEDIATE);
    }

    // ------------------------------------------------------------
    // 이벤트 (지시서 11장 [플레이어 생명주기])
    // ------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        loadFor(event.getPlayer());
    }

    private void loadFor(Player player) {
        UUID uuid = player.getUniqueId();
        if (online.containsKey(uuid)) {
            return;
        }
        saves.loadAsync(uuid).whenComplete((data, error) -> {
            if (error != null) {
                logger.log(Level.SEVERE, "플레이어 데이터를 읽지 못했습니다: " + uuid, error);
                return;
            }
            saves.mainThread().run(() -> attach(player, data));
        });
    }

    private void attach(Player player, PlayerData data) {
        if (!player.isOnline()) {
            // 읽는 동안 나갔다. 캐시에 올리지 않는다.
            return;
        }
        data.name(player.getName());
        data.lastLogin(System.currentTimeMillis());
        cache.put(data);
        online.put(data.uuid(), new RpgPlayer(player, data));
        // TODO 2단계: UI(액션바·스코어보드·탭) 초기화를 여기에 붙인다.
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        online.remove(uuid);
        PlayerData data = cache.get(uuid);
        if (data == null) {
            // 아직 로드가 끝나지 않은 상태. 저장할 것이 없다.
            return;
        }
        saves.saveAndRelease(data);
    }
}
