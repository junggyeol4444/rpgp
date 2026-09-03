package com.example.rpgcore.quest.reward;

import com.example.rpgcore.level.CombatLevelService;
import com.example.rpgcore.level.ExpSource;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.player.data.PlayerData;
import com.example.rpgcore.storage.dirty.SavePriority;
import com.example.rpgcore.storage.dirty.SaveScheduler;
import java.util.Map;

/**
 * 지시서 3장 [quest/reward] — 보상 지급.
 *
 * <p>퀘스트 완료 처리와 보상 지급은 즉시 저장 대상이다.
 * (지시서 5장 [저장 정책] / 기획서 9장)
 */
public final class RewardService {

    private final CombatLevelService levels;
    private final SaveScheduler saves;

    public RewardService(CombatLevelService levels, SaveScheduler saves) {
        this.levels = levels;
        this.saves = saves;
    }

    /**
     * 보상을 준다.
     *
     * <p>TODO 6단계: 재화 지급은 CurrencyService 를 거치도록 바꾼다.
     *      지금은 저장 데이터의 currency 를 직접 다룬다.
     */
    public void grant(RpgPlayer rpgPlayer, QuestReward reward) {
        if (reward.isEmpty()) {
            return;
        }
        PlayerData data = rpgPlayer.data();

        if (reward.skillPoints() > 0) {
            data.skill().points(data.skill().points() + reward.skillPoints());
        }
        if (reward.statPoints() > 0) {
            data.combat().statPoints(data.combat().statPoints() + reward.statPoints());
        }
        for (Map.Entry<String, Long> entry : reward.currency().entrySet()) {
            long owned = data.currency(entry.getKey());
            data.currency().put(entry.getKey(), owned + Math.max(0, entry.getValue()));
        }
        saves.markDirty(data, SavePriority.IMMEDIATE);

        // 경험치는 레벨업 판정까지 태워야 하므로 마지막에 준다.
        if (reward.combatExp() > 0) {
            levels.addExp(rpgPlayer, reward.combatExp(), ExpSource.QUEST_COMPLETE);
        }
    }
}
