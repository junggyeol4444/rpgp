package com.example.rpgcore.storage.dirty;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 지시서 3장 [storage/dirty] / 4장 [변경 추적].
 *
 * <p>지연 저장 대상으로 표시된 플레이어를 모아둔다.
 * 즉시 저장 대상은 여기 쌓이지 않고 곧바로 저장 큐로 간다.
 */
public final class DirtyTracker {

    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();

    public void mark(UUID uuid) {
        dirty.add(uuid);
    }

    public void clear(UUID uuid) {
        dirty.remove(uuid);
    }

    public boolean isDirty(UUID uuid) {
        return dirty.contains(uuid);
    }

    public int size() {
        return dirty.size();
    }

    /** 쌓인 목록을 가져가면서 비운다. */
    public Set<UUID> drain() {
        Set<UUID> snapshot = new HashSet<>(dirty);
        dirty.removeAll(snapshot);
        return snapshot;
    }
}
