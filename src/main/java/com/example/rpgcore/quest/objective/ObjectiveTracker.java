package com.example.rpgcore.quest.objective;

import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.quest.QuestService;

/**
 * 지시서 11장 [퀘스트] — 목표 판정 계층.
 *
 * <p>퀘스트 코드가 이벤트를 직접 듣지 않도록, 이벤트와 퀘스트 사이에
 * 이 계층을 둔다. 리스너는 "무엇이 일어났는지"만 알려주고,
 * 그것이 어느 퀘스트의 어느 목표인지는 여기 아래에서 정한다.
 */
public final class ObjectiveTracker {

    private final QuestService quests;

    public ObjectiveTracker(QuestService quests) {
        this.quests = quests;
    }

    /**
     * 일어난 일을 알린다.
     *
     * @param key    대상 식별자. 개체 종류 · 아이템 · 지역 id · NPC id
     * @param amount 올릴 양
     */
    public void report(RpgPlayer rpgPlayer, ObjectiveType type, String key, int amount) {
        if (rpgPlayer == null || key == null || amount <= 0) {
            return;
        }
        if (rpgPlayer.data().quest().active().isEmpty()) {
            // 진행 중인 퀘스트가 없으면 더 볼 것이 없다.
            return;
        }
        quests.report(rpgPlayer, type, key, amount);
    }

    /** 진행 중인 퀘스트가 있는지. 리스너가 미리 걸러낼 때 쓴다. */
    public boolean hasActive(RpgPlayer rpgPlayer) {
        return rpgPlayer != null && !rpgPlayer.data().quest().active().isEmpty();
    }
}
