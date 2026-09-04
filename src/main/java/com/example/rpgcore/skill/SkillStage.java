package com.example.rpgcore.skill;

/**
 * 지시서 8장 [skills.yml] 의 stage 값.
 *
 * <p>기획서 6장: 기본 직업 단계 6~8개, 전직할 때마다 6~8개 추가.
 * TIER1/TIER2 스킬은 해당 전직을 하지 않으면 해금할 수 없다.
 */
public enum SkillStage {

    /** 기본 직업 단계 */
    BASE(1),
    /** 1차 전직 단계 */
    TIER1(2),
    /** 2차 전직 단계 */
    TIER2(3);

    private final int requiredJobStage;

    SkillStage(int requiredJobStage) {
        this.requiredJobStage = requiredJobStage;
    }

    /**
     * 해금하려면 필요한 직업 단계.
     * {@code PlayerData.Job#stage()} 와 비교한다.
     */
    public int requiredJobStage() {
        return requiredJobStage;
    }

    /** 이름으로 찾는다. 없으면 null. */
    public static SkillStage fromConfig(String value) {
        if (value == null) {
            return null;
        }
        for (SkillStage stage : values()) {
            if (stage.name().equalsIgnoreCase(value)) {
                return stage;
            }
        }
        return null;
    }
}
