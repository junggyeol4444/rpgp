package com.example.rpgcore.skill.effect;

/**
 * 지시서 8장 [규칙]: 스킬 효과 타입은 열거형으로 정의하고,
 * 타입마다 실행기 클래스를 둔다. 설정에 없는 타입이 오면 해당 스킬만
 * 비활성화하고 로그를 남긴다.
 */
public enum EffectType {

    /** 정면 부채꼴 범위 피해 */
    DAMAGE_CONE,
    /** 단일 대상 피해 */
    DAMAGE_TARGET,
    /** 자기 주위 원형 범위 피해 */
    DAMAGE_AREA,
    /** 자신 회복 */
    HEAL_SELF;

    /** 이름으로 찾는다. 없으면 null. */
    public static EffectType fromConfig(String value) {
        if (value == null) {
            return null;
        }
        for (EffectType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
