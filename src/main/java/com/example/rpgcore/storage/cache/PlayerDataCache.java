package com.example.rpgcore.storage.cache;

import com.example.rpgcore.player.data.PlayerData;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 지시서 3장 [storage/cache] — 접속 중 플레이어 캐시.
 *
 * <p>접속할 때 비동기로 읽어온 데이터를 여기에 올리고, 퇴장할 때 내린다.
 * 저장 스케줄러가 다른 스레드에서 목록을 훑을 수 있으므로
 * 동시성 컬렉션을 쓴다.
 */
public final class PlayerDataCache {

    private final Map<UUID, PlayerData> cached = new ConcurrentHashMap<>();

    public void put(PlayerData data) {
        cached.put(data.uuid(), data);
    }

    /** 없으면 null. 아직 로드가 끝나지 않은 상태일 수 있다. */
    public PlayerData get(UUID uuid) {
        return cached.get(uuid);
    }

    public boolean contains(UUID uuid) {
        return cached.containsKey(uuid);
    }

    public PlayerData remove(UUID uuid) {
        return cached.remove(uuid);
    }

    public Collection<PlayerData> all() {
        return cached.values();
    }

    public int size() {
        return cached.size();
    }

    public void clear() {
        cached.clear();
    }
}
