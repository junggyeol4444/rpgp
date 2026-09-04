package com.example.rpgcore.life.unlock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 지시서 8장 [life.yml] 의 rewards 블록.
 *
 * <p>기획서 3장: 트랙 레벨업 보상은 두 가지를 모두 준다.
 * 해당 분야 효율 상승, 그리고 레시피 · 콘텐츠 해금.
 *
 * @param efficiencyPerLevel 효율 이름 -> 레벨당 상승폭
 * @param unlockAtLevel      레벨 -> 그 레벨에서 열리는 id 목록
 */
public record TrackReward(Map<String, Double> efficiencyPerLevel,
                          Map<Integer, List<String>> unlockAtLevel) {

    public TrackReward {
        efficiencyPerLevel = Map.copyOf(efficiencyPerLevel);
        Map<Integer, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<String>> entry : unlockAtLevel.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        unlockAtLevel = copy;
    }

    public static TrackReward none() {
        return new TrackReward(Map.of(), Map.of());
    }

    /** 지정한 레벨에서 얻는 효율값. */
    public double efficiency(String name, int level) {
        Double perLevel = efficiencyPerLevel.get(name);
        return perLevel == null ? 0 : perLevel * Math.max(0, level);
    }

    /**
     * 지정한 레벨까지 열리는 id 전부.
     * 레벨을 건너뛰고 올라가도 빠지지 않도록 이하 전부를 모은다.
     */
    public List<String> unlockedUpTo(int level) {
        List<String> ids = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> entry : unlockAtLevel.entrySet()) {
            if (entry.getKey() <= level) {
                ids.addAll(entry.getValue());
            }
        }
        return ids;
    }
}
