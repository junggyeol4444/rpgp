package com.example.rpgcore.stat;

/**
 * 지시서 4장 [스탯 구조] — 능력치를 환산해서 나오는 실제 수치.
 *
 * <p>능력치(힘·민첩·지능·체력)는 stats.yml 에서 자유롭게 정의할 수 있지만,
 * 환산 결과인 이 목록은 전투 계산이 직접 참조하므로 고정한다.
 * stats.yml 에 여기 없는 키가 오면 해당 항목만 건너뛰고 로그를 남긴다.
 */
public enum DerivedStat {

    /** 물리 공격력 */
    PHYSICAL_DAMAGE("physicalDamage"),
    /** 마법 공격력 */
    MAGIC_DAMAGE("magicDamage"),
    /** 방어력 */
    DEFENSE("defense"),
    /** 최대 내부 HP. 바닐라 하트 20칸과는 별개다. (지시서 9장) */
    MAX_HEALTH("maxHealth"),
    /** 최대 마나 */
    MAX_MANA("maxMana"),
    /** 치명타 확률. 0.0 ~ 1.0 */
    CRIT_CHANCE("critChance"),
    /** 치명타 배수 */
    CRIT_DAMAGE("critDamage"),
    /** 공격 속도 */
    ATTACK_SPEED("attackSpeed");

    private final String configKey;

    DerivedStat(String configKey) {
        this.configKey = configKey;
    }

    public String configKey() {
        return configKey;
    }

    /** 설정 키로 찾는다. 없으면 null. */
    public static DerivedStat fromConfigKey(String key) {
        if (key == null) {
            return null;
        }
        for (DerivedStat stat : values()) {
            if (stat.configKey.equals(key)) {
                return stat;
            }
        }
        return null;
    }
}
