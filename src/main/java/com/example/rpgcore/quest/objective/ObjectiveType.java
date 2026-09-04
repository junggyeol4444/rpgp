package com.example.rpgcore.quest.objective;

/**
 * 지시서 8장 [quests.yml] 의 objectives.type.
 *
 * <p>기획서 7장 [퀘스트 유형]: 처치 / 수집·납품 / 배달·이동 /
 * 대화·스토리 진행.
 */
public enum ObjectiveType {

    /** 처치. target(개체 종류) + amount */
    KILL("target"),
    /** 수집. item + amount */
    COLLECT("item"),
    /** 이동. region */
    REACH("region"),
    /** 대화. npc */
    TALK("npc");

    private final String keyField;

    ObjectiveType(String keyField) {
        this.keyField = keyField;
    }

    /** 대상을 적는 설정 키 이름. */
    public String keyField() {
        return keyField;
    }

    /** 이름으로 찾는다. 없으면 null. */
    public static ObjectiveType fromConfig(String value) {
        if (value == null) {
            return null;
        }
        for (ObjectiveType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
