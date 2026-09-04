package com.example.rpgcore.stat;

import java.util.Map;

/**
 * 지시서 3장 [stat/StatType] — 능력치 하나의 정의.
 *
 * <p>stats.yml 에서 읽는다. 종류와 환산 계수를 운영자가 바꿀 수 있으므로
 * 열거형이 아니라 설정에서 만들어지는 값이다. (지시서 0장 3번)
 *
 * @param id       능력치 id. 저장 파일의 combat.stats 키와 같다
 * @param display  표시 이름
 * @param perPoint 1포인트당 올라가는 파생 수치
 * @param order    stats.yml 에 적힌 순서. GUI 배치에 쓴다
 */
public record StatType(String id, String display, Map<DerivedStat, Double> perPoint, int order) {

    public StatType {
        perPoint = Map.copyOf(perPoint);
    }

    /** 1포인트당 올라가는 양. 없으면 0. */
    public double perPoint(DerivedStat stat) {
        Double value = perPoint.get(stat);
        return value == null ? 0.0 : value;
    }
}
