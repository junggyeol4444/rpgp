package com.example.rpgcore.config.schema;

/**
 * config.yml 의 combat 블록. 2단계에서 추가했다.
 *
 * <p>지시서 0장 3번에 따라 데미지 계수를 코드에 두지 않는다.
 *
 * @param defenseConstant 방어력 감쇠 상수.
 *                        실제 피해 = 피해 x c / (c + 방어력)
 * @param minimumDamage   방어력이 아무리 높아도 들어가는 최소 피해
 * @param pvpEnabled      플레이어끼리의 피해를 계산할지
 */
public record CombatSettings(double defenseConstant, double minimumDamage, boolean pvpEnabled) {

    public static CombatSettings defaults() {
        return new CombatSettings(100.0, 1.0, true);
    }

    /** 방어력을 반영한 피해량. */
    public double applyDefense(double damage, double defense) {
        double constant = Math.max(1.0, defenseConstant);
        double effective = Math.max(0.0, defense);
        double reduced = damage * constant / (constant + effective);
        return Math.max(minimumDamage, reduced);
    }
}
