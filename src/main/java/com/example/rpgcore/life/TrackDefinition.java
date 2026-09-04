package com.example.rpgcore.life;

import java.util.EnumMap;
import java.util.Map;

/**
 * 지시서 8장 [life.yml] 의 트랙 하나.
 *
 * <p>획득원은 두 가지 모양을 받는다.
 * <pre>
 *   BLOCK_BREAK: { LOG: 5, WHEAT: 3 }   # 대상별로 다르게
 *   FISHING: 8                          # 대상 구분 없이
 * </pre>
 * 뒤쪽은 {@link #DEFAULT_KEY} 하나만 가진 표로 저장한다.
 *
 * @param type    트랙
 * @param display 표시 이름
 * @param sources 획득원 -> (대상 -> 경험치)
 */
public record TrackDefinition(TrackType type, String display,
                              Map<LifeSource, Map<String, Double>> sources) {

    /** 대상을 가리지 않는 획득원이 쓰는 키. */
    public static final String DEFAULT_KEY = "*";

    public TrackDefinition {
        Map<LifeSource, Map<String, Double>> copy = new EnumMap<>(LifeSource.class);
        for (Map.Entry<LifeSource, Map<String, Double>> entry : sources.entrySet()) {
            copy.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        sources = copy;
    }

    /**
     * 이 획득원에서 받을 경험치.
     *
     * <p>대상별 값이 있으면 그것을, 없으면 기본값을 쓴다.
     * 둘 다 없으면 0이다.
     *
     * @param key 대상 이름 (블록 · 아이템 종류 등). 없으면 null
     */
    public double exp(LifeSource source, String key) {
        Map<String, Double> byKey = sources.get(source);
        if (byKey == null) {
            return 0;
        }
        if (key != null) {
            Double exact = byKey.get(key);
            if (exact != null) {
                return exact;
            }
        }
        Double fallback = byKey.get(DEFAULT_KEY);
        return fallback == null ? 0 : fallback;
    }

    /** 이 획득원을 아예 안 쓰는 트랙인지. */
    public boolean handles(LifeSource source) {
        return sources.containsKey(source);
    }
}
