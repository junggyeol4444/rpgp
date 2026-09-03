package com.example.rpgcore.quest;

/**
 * 지시서 8장 [quests.yml] 의 type 값.
 *
 * <p>기획서 7장 [구성]: 메인 스토리 라인 + 사이드 퀘스트 +
 * 반복 가능한 일일·주간 퀘스트.
 */
public enum QuestType {

    /** 사이드 퀘스트 */
    NORMAL(false),
    /** 메인 스토리 */
    MAIN(false),
    /** 하루마다 다시 받을 수 있다 */
    DAILY(true),
    /** 한 주마다 다시 받을 수 있다 */
    WEEKLY(true);

    private final boolean cyclic;

    QuestType(boolean cyclic) {
        this.cyclic = cyclic;
    }

    /** 주기마다 초기화되는 종류인지. */
    public boolean cyclic() {
        return cyclic;
    }

    /** 이름으로 찾는다. 없으면 null. */
    public static QuestType fromConfig(String value) {
        if (value == null) {
            return null;
        }
        for (QuestType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
