package com.example.rpgcore.quest.reward;

import com.example.rpgcore.economy.CurrencyService;
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
    private final CurrencyService currencies;

    public RewardService(CombatLevelService levels, SaveScheduler saves,
                         CurrencyService currencies) {
        this.levels = levels;
        this.saves = saves;
        this.currencies = currencies;
    }

    /**
     * 보상을 준다.
     *
     * <p>재화 지급은 {@link CurrencyService} 를 거친다. (6단계)
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
        saves.markDirty(data, SavePriority.IMMEDIATE);
        for (Map.Entry<String, Long> entry : reward.currency().entrySet()) {
            currencies.deposit(rpgPlayer, entry.getKey(), entry.getValue());
        }

        // 경험치는 레벨업 판정까지 태워야 하므로 마지막에 준다.
        if (reward.combatExp() > 0) {
            levels.addExp(rpgPlayer, reward.combatExp(), ExpSource.QUEST_COMPLETE);
        }
    }
}
