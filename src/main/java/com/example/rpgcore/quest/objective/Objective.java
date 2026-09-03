package com.example.rpgcore.quest.objective;

/**
 * 목표 하나.
 *
 * @param type   목표 종류
 * @param key    대상 식별자. 개체 종류 · 아이템 · 지역 id · NPC id
 * @param amount 필요한 개수. REACH · TALK 은 1이다
 */
public record Objective(ObjectiveType type, String key, int amount) {

    public Objective {
        amount = Math.max(1, amount);
    }

    /** 진행도가 다 찼는지. */
    public boolean isComplete(int progress) {
        return progress >= amount;
    }
}
