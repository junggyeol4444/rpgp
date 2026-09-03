package com.example.rpgcore.quest.reward;

import java.util.Map;

/**
 * 지시서 8장 [quests.yml] 의 rewards 블록.
 *
 * @param combatExp   전투 경험치
 * @param skillPoints 스킬 포인트
 * @param statPoints  스탯 포인트. 지시서 예시에는 없지만 같은 자리에 둔다
 * @param currency    특수 재화 id -> 지급량
 */
public record QuestReward(double combatExp, int skillPoints, int statPoints,
                          Map<String, Long> currency) {

    public QuestReward {
        currency = Map.copyOf(currency);
    }

    public static QuestReward none() {
        return new QuestReward(0, 0, 0, Map.of());
    }

    public boolean isEmpty() {
        return combatExp <= 0 && skillPoints <= 0 && statPoints <= 0 && currency.isEmpty();
    }
}
