package com.example.rpgcore.life;

/**
 * 지시서 3장 / 기획서 3장.
 *
 * <p>생활 계열 경험치 트랙 4종. 전투 레벨과 별개로 각각 독립 레벨을 가진다.
 * 트랙 레벨 상한은 없다.
 *
 * <p>실제 트랙 동작은 7단계에서 구현한다. 여기서는 데이터 스키마(7장)에
 * 필요한 식별자만 정의한다.
 */
public enum TrackType {

    /** 벌목 · 농사 · 낚시 · 요리 */
    LIVING("living"),
    /** 광물 채굴 */
    MINING("mining"),
    /** 조합대로 만드는 제작 전반 */
    CRAFTING("crafting"),
    /** 연금 · 물약 */
    ALCHEMY("alchemy");

    private final String configKey;

    TrackType(String configKey) {
        this.configKey = configKey;
    }

    /** life.yml 및 저장 파일에서 쓰는 키. */
    public String configKey() {
        return configKey;
    }

    /** 설정 키로 트랙을 찾는다. 없으면 null. */
    public static TrackType fromConfigKey(String key) {
        if (key == null) {
            return null;
        }
        for (TrackType type : values()) {
            if (type.configKey.equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }
}
