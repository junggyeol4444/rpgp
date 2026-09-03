package com.example.rpgcore.skill.effect;

import java.util.Map;

/**
 * 효과 하나의 설정값.
 *
 * <p>타입마다 필요한 값이 달라서 이름-값 표로 들고 있고,
 * 해석은 각 실행기가 한다.
 *
 * @param type   효과 종류
 * @param values 설정에 적힌 값들 (range, angle 등)
 */
public record SkillEffect(EffectType type, Map<String, Double> values) {

    public SkillEffect {
        values = Map.copyOf(values);
    }

    /** 없으면 기본값. */
    public double value(String key, double fallback) {
        Double found = values.get(key);
        return found == null ? fallback : found;
    }
}
