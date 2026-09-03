package com.example.rpgcore.config.schema;

import java.util.Set;

/**
 * config.yml 의 ui 블록. 2단계에서 추가했다.
 *
 * @param updateIntervalTicks 상시 표시 갱신 주기 (틱)
 * @param channels            켜져 있는 표시 채널 id
 */
public record UiSettings(long updateIntervalTicks, Set<String> channels) {

    public UiSettings {
        updateIntervalTicks = Math.max(1, updateIntervalTicks);
        channels = Set.copyOf(channels);
    }

    public static UiSettings defaults() {
        return new UiSettings(20, Set.of("actionbar", "scoreboard", "tab", "bossbar"));
    }
}
