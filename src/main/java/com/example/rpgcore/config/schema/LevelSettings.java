package com.example.rpgcore.config.schema;

import com.example.rpgcore.level.ExpSource;
import java.util.EnumSet;
import java.util.Set;

/**
 * 지시서 8장 [levels.yml] 의 파싱 결과.
 *
 * @param combatCurve        전투 레벨 곡선
 * @param combatMaxLevel     -1 이면 상한 없음
 * @param statPointsPerLevel 레벨업마다 주는 스탯 포인트
 * @param skillPointsPerLevel 레벨업마다 주는 스킬 포인트
 * @param enabledExpSources  켜져 있는 경험치 획득원
 * @param lifeCurve          생활 트랙 곡선 (7단계에서 사용)
 * @param lifeMaxLevel       -1 이면 상한 없음
 */
public record LevelSettings(CurveSettings combatCurve,
                            int combatMaxLevel,
                            int statPointsPerLevel,
                            int skillPointsPerLevel,
                            Set<ExpSource> enabledExpSources,
                            CurveSettings lifeCurve,
                            int lifeMaxLevel) {

    public LevelSettings {
        enabledExpSources = Set.copyOf(enabledExpSources);
    }

    /** levels.yml 을 읽지 못했을 때 쓰는 기본값. */
    public static LevelSettings defaults() {
        return new LevelSettings(
                CurveSettings.defaultCombat(),
                -1,
                5,
                1,
                EnumSet.allOf(ExpSource.class),
                CurveSettings.defaultLife(),
                -1);
    }

    /** 해당 획득원으로 경험치를 줄 수 있는지. */
    public boolean isEnabled(ExpSource source) {
        return !source.configurable() || enabledExpSources.contains(source);
    }

    /** 전투 레벨에 상한이 있는지. */
    public boolean hasCombatMaxLevel() {
        return combatMaxLevel > 0;
    }
}
