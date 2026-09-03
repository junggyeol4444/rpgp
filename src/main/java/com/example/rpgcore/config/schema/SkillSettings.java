package com.example.rpgcore.config.schema;

/**
 * config.yml 의 skill 블록. 4단계에서 추가했다.
 *
 * @param manaRegenPerSecond   초당 마나 회복량.
 *                             기획서에 회복 방식이 적혀 있지 않아 설정으로 뺐다
 * @param baseItemSlots        스킬 아이템 슬롯 시작 칸 수 (기획서 6장: 2)
 * @param slotsPerAdvancement  전직할 때마다 늘어나는 칸 수 (기획서 6장: 1)
 * @param unlockCost           스킬 하나를 해금하는 데 드는 스킬 포인트.
 *                             기획서 6장은 해금을 트리로 막고 레벨을
 *                             포인트로 올리는 구조라 기본값은 0이다
 */
public record SkillSettings(double manaRegenPerSecond,
                            int baseItemSlots,
                            int slotsPerAdvancement,
                            int unlockCost) {

    public static SkillSettings defaults() {
        return new SkillSettings(1.0, 2, 1, 0);
    }

    /**
     * 지금 열려 있는 아이템 슬롯 수.
     *
     * <p>기획서 6장: 시작 2칸, 전직할 때마다 +1
     * (1차 전직 후 3칸, 2차 전직 후 4칸).
     *
     * @param jobStage 0 미선택 / 1 기본 / 2 1차 / 3 2차
     */
    public int slotCount(int jobStage) {
        int advancements = Math.max(0, jobStage - 1);
        return baseItemSlots + slotsPerAdvancement * advancements;
    }
}
