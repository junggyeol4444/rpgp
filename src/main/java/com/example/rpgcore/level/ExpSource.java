package com.example.rpgcore.level;

/**
 * 지시서 3장 / 기획서 3장 [경험치 획득원].
 *
 * <p>levels.yml 의 combat.expSources 에서 획득원별로 켜고 끌 수 있다.
 */
public enum ExpSource {

    /** 몬스터 처치 */
    MOB_KILL("mobKill"),
    /** 퀘스트 완료 */
    QUEST_COMPLETE("questComplete"),
    /** 던전·보스 클리어 */
    DUNGEON_CLEAR("dungeonClear"),
    /** 관리자 명령. 설정으로 끌 수 없다. */
    ADMIN(null);

    private final String configKey;

    ExpSource(String configKey) {
        this.configKey = configKey;
    }

    /** levels.yml 의 키. ADMIN 은 설정 대상이 아니므로 null. */
    public String configKey() {
        return configKey;
    }

    /** 설정으로 끌 수 있는 획득원인지. */
    public boolean configurable() {
        return configKey != null;
    }
}
