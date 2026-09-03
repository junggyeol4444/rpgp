package com.example.rpgcore.config.schema;

import com.example.rpgcore.stat.DerivedStat;
import com.example.rpgcore.stat.StatType;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 지시서 8장 [stats.yml] 의 파싱 결과.
 *
 * @param stats 능력치 id -> 정의. stats.yml 에 적힌 순서를 지킨다
 * @param base  능력치가 0일 때의 기본 수치
 * @param reset 스탯 초기화 규칙
 */
public record StatSettings(Map<String, StatType> stats,
                           Map<DerivedStat, Double> base,
                           ResetSettings reset) {

    public StatSettings {
        stats = new LinkedHashMap<>(stats);
        base = new EnumMap<>(base);
    }

    /** stats.yml 을 읽지 못했을 때 쓰는 값. 능력치가 하나도 없는 상태다. */
    public static StatSettings defaults() {
        return new StatSettings(new LinkedHashMap<>(),
                new EnumMap<>(DerivedStat.class),
                ResetSettings.defaults());
    }

    /** 없으면 null. */
    public StatType stat(String id) {
        return stats.get(id);
    }

    public double base(DerivedStat stat) {
        Double value = base.get(stat);
        return value == null ? 0.0 : value;
    }
}
