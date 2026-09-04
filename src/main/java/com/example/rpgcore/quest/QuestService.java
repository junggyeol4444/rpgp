package com.example.rpgcore.quest;

import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.core.Lifecycle;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.player.data.PlayerData;
import com.example.rpgcore.quest.objective.Objective;
import com.example.rpgcore.quest.objective.ObjectiveType;
import com.example.rpgcore.quest.repeat.ResetCycle;
import com.example.rpgcore.quest.reward.RewardService;
import com.example.rpgcore.storage.dirty.SavePriority;
import com.example.rpgcore.storage.dirty.SaveScheduler;
import com.example.rpgcore.util.Messages;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 지시서 3장 [quest/QuestService] — 수주 · 진행 · 완료.
 *
 * <p>이벤트를 직접 듣지 않는다. 목표 판정 계층이 걸러서 넘겨준 것만
 * 처리한다. (지시서 11장 [퀘스트])
 *
 * <p>퀘스트 완료 처리와 보상 지급은 즉시 저장한다. 수주와 진행도는
 * 지연 저장이다. (지시서 5장 [저장 정책])
 */
public final class QuestService implements Lifecycle {

    /** 수주 · 완료 시도의 결과. */
    public enum Result {
        OK,
        UNKNOWN_QUEST,
        /** 이미 진행 중 */
        ALREADY_ACTIVE,
        /** 이미 완료했고 반복 불가 */
        ALREADY_COMPLETED,
        /** 레벨이 모자람 */
        LEVEL_TOO_LOW,
        /** 직업 조건이 맞지 않음 */
        WRONG_JOB,
        /** 진행 중이 아님 */
        NOT_ACTIVE,
        /** 목표를 다 채우지 못함 */
        NOT_COMPLETE
    }

    private final ConfigManager config;
    private final SaveScheduler saves;
    private final RewardService rewards;
    private final Messages messages;

    public QuestService(ConfigManager config, SaveScheduler saves,
                        RewardService rewards, Messages messages) {
        this.config = config;
        this.saves = saves;
        this.rewards = rewards;
        this.messages = messages;
    }

    @Override
    public String serviceName() {
        return "QuestService";
    }

    public Map<String, QuestDefinition> definitions() {
        return config.quests();
    }

    /** 없으면 null. */
    public QuestDefinition definition(String questId) {
        return config.quests().get(questId);
    }

    // ------------------------------------------------------------
    // 수주
    // ------------------------------------------------------------

    /** 지금 받을 수 있는지. */
    public Result canAccept(PlayerData data, QuestDefinition quest) {
        if (data.quest().active().containsKey(quest.id())) {
            return Result.ALREADY_ACTIVE;
        }
        if (data.quest().completed().contains(quest.id()) && !quest.canRepeat()) {
            return Result.ALREADY_COMPLETED;
        }
        if (data.combat().level() < quest.requireLevel()) {
            return Result.LEVEL_TOO_LOW;
        }
        if (quest.requireJob() != null && !quest.requireJob().equals(data.job().base())) {
            return Result.WRONG_JOB;
        }
        return Result.OK;
    }

    /** 퀘스트를 받는다. 기획서 7장: 동시 수행 제한은 두지 않는다. */
    public Result accept(RpgPlayer rpgPlayer, String questId) {
        QuestDefinition quest = definition(questId);
        if (quest == null) {
            return Result.UNKNOWN_QUEST;
        }
        PlayerData data = rpgPlayer.data();
        Result check = canAccept(data, quest);
        if (check != Result.OK) {
            return check;
        }
        QuestProgress progress =
                new QuestProgress(System.currentTimeMillis(), quest.objectives().size());
        data.quest().active().put(questId, progress.toMap());
        saves.markDirty(data, SavePriority.DEFERRED);
        messages.send(rpgPlayer.player(), "quest.accepted", "quest", quest.display());
        return Result.OK;
    }

    /** 퀘스트를 포기한다. 진행도는 사라진다. */
    public Result abandon(RpgPlayer rpgPlayer, String questId) {
        PlayerData data = rpgPlayer.data();
        if (data.quest().active().remove(questId) == null) {
            return Result.NOT_ACTIVE;
        }
        saves.markDirty(data, SavePriority.DEFERRED);
        return Result.OK;
    }

    // ------------------------------------------------------------
    // 진행
    // ------------------------------------------------------------

    /** 진행 중인 퀘스트의 진행도. 없으면 null. */
    public QuestProgress progressOf(PlayerData data, QuestDefinition quest) {
        Object raw = data.quest().active().get(quest.id());
        if (raw == null) {
            return null;
        }
        return QuestProgress.fromMap(asMap(raw), quest.objectives().size());
    }

    /** 진행 중인 퀘스트 id 목록. */
    public List<String> activeIds(PlayerData data) {
        return new ArrayList<>(data.quest().active().keySet());
    }

    /**
     * 목표 판정 계층이 부른다.
     *
     * <p>진행 중인 퀘스트 가운데 이 종류·대상에 걸리는 목표를 올린다.
     * 목표를 다 채우면 자동으로 완료 처리한다.
     *
     * @param key    대상 식별자. 대소문자를 가리지 않는다
     * @param amount 올릴 양
     */
    public void report(RpgPlayer rpgPlayer, ObjectiveType type, String key, int amount) {
        if (key == null || amount <= 0) {
            return;
        }
        PlayerData data = rpgPlayer.data();
        if (data.quest().active().isEmpty()) {
            return;
        }

        List<String> finished = new ArrayList<>();
        boolean changed = false;

        for (String questId : activeIds(data)) {
            QuestDefinition quest = definition(questId);
            if (quest == null) {
                continue;
            }
            QuestProgress progress = progressOf(data, quest);
            if (progress == null) {
                continue;
            }
            boolean touched = false;
            for (int i = 0; i < quest.objectives().size(); i++) {
                Objective objective = quest.objectives().get(i);
                if (objective.type() != type || !objective.key().equalsIgnoreCase(key)) {
                    continue;
                }
                if (objective.isComplete(progress.count(i))) {
                    continue;
                }
                if (progress.advance(i, amount)) {
                    touched = true;
                }
            }
            if (!touched) {
                continue;
            }
            data.quest().active().put(questId, progress.toMap());
            changed = true;
            if (isComplete(quest, progress)) {
                finished.add(questId);
            }
        }

        if (changed) {
            saves.markDirty(data, SavePriority.DEFERRED);
        }
        for (String questId : finished) {
            complete(rpgPlayer, questId);
        }
    }

    /** 목표를 다 채웠는지. */
    public boolean isComplete(QuestDefinition quest, QuestProgress progress) {
        for (int i = 0; i < quest.objectives().size(); i++) {
            if (!quest.objectives().get(i).isComplete(progress.count(i))) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------
    // 완료
    // ------------------------------------------------------------

    /** 완료 처리하고 보상을 준다. */
    public Result complete(RpgPlayer rpgPlayer, String questId) {
        QuestDefinition quest = definition(questId);
        if (quest == null) {
            return Result.UNKNOWN_QUEST;
        }
        PlayerData data = rpgPlayer.data();
        QuestProgress progress = progressOf(data, quest);
        if (progress == null) {
            return Result.NOT_ACTIVE;
        }
        if (!isComplete(quest, progress)) {
            return Result.NOT_COMPLETE;
        }

        data.quest().active().remove(questId);
        if (!data.quest().completed().contains(questId)) {
            data.quest().completed().add(questId);
        }
        // 완료 처리와 보상 지급은 즉시 저장한다.
        saves.markDirty(data, SavePriority.IMMEDIATE);
        rewards.grant(rpgPlayer, quest.reward());
        messages.send(rpgPlayer.player(), "quest.completed", "quest", quest.display());
        return Result.OK;
    }

    /** 관리자용 강제 완료. 목표를 다 채운 것으로 보고 보상까지 준다. */
    public Result forceComplete(RpgPlayer rpgPlayer, String questId) {
        QuestDefinition quest = definition(questId);
        if (quest == null) {
            return Result.UNKNOWN_QUEST;
        }
        PlayerData data = rpgPlayer.data();
        QuestProgress progress = progressOf(data, quest);
        if (progress == null) {
            progress = new QuestProgress(System.currentTimeMillis(), quest.objectives().size());
        }
        for (int i = 0; i < quest.objectives().size(); i++) {
            progress.set(i, quest.objectives().get(i).amount());
        }
        data.quest().active().put(questId, progress.toMap());
        return complete(rpgPlayer, questId);
    }

    // ------------------------------------------------------------
    // 일일 · 주간 리셋
    // ------------------------------------------------------------

    /**
     * 주기가 지났으면 해당 종류의 완료 이력을 지운다.
     *
     * <p>접속할 때와 관리자 명령에서 부른다.
     *
     * @return 지워진 퀘스트 수
     */
    public int applyCycles(RpgPlayer rpgPlayer, long now) {
        PlayerData data = rpgPlayer.data();
        int cleared = 0;
        if (ResetCycle.DAILY.isDue(data.quest().dailyResetAt(), now)) {
            cleared += clearCycle(data, QuestType.DAILY);
            data.quest().dailyResetAt(now);
        }
        if (ResetCycle.WEEKLY.isDue(data.quest().weeklyResetAt(), now)) {
            cleared += clearCycle(data, QuestType.WEEKLY);
            data.quest().weeklyResetAt(now);
        }
        if (cleared > 0) {
            saves.markDirty(data, SavePriority.DEFERRED);
        }
        return cleared;
    }

    /** 관리자용 강제 리셋. (/rpg admin questcycle) */
    public int resetCycle(RpgPlayer rpgPlayer, QuestType type, long now) {
        PlayerData data = rpgPlayer.data();
        int cleared = clearCycle(data, type);
        if (type == QuestType.DAILY) {
            data.quest().dailyResetAt(now);
        } else if (type == QuestType.WEEKLY) {
            data.quest().weeklyResetAt(now);
        }
        saves.markDirty(data, SavePriority.DEFERRED);
        return cleared;
    }

    private int clearCycle(PlayerData data, QuestType type) {
        int cleared = 0;
        for (String questId : new ArrayList<>(data.quest().completed())) {
            QuestDefinition quest = definition(questId);
            if (quest != null && quest.type() == type) {
                data.quest().completed().remove(questId);
                cleared++;
            }
        }
        return cleared;
    }

    /** 진행 이력을 전부 지운다. (/rpg admin questreset) */
    public void resetAll(RpgPlayer rpgPlayer) {
        PlayerData data = rpgPlayer.data();
        data.quest().active().clear();
        data.quest().completed().clear();
        data.quest().dailyResetAt(0);
        data.quest().weeklyResetAt(0);
        saves.markDirty(data, SavePriority.IMMEDIATE);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return Map.of();
    }
}
