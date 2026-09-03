package com.example.rpgcore.skill;

/**
 * 지시서 8장 [skills.yml] 의 power 블록.
 *
 * <p>기획서 6장: 스킬 레벨 상한은 9999 이고, 레벨당 위력 상승폭은
 * 고레벨에서 완만해지도록 감쇠를 둔다.
 *
 * <p>{@code power = base + coefficient * level^exponent}
 *
 * @param base        1레벨 위력
 * @param type        감쇠 방식. 현재 DIMINISHING 만 구현되어 있다
 * @param coefficient 레벨 항의 계수
 * @param exponent    레벨 지수. 1보다 작으면 고레벨에서 완만해진다
 */
public record PowerScaling(double base, String type, double coefficient, double exponent) {

    public static final String DIMINISHING = "DIMINISHING";

    public static PowerScaling defaults() {
        return new PowerScaling(0.0, DIMINISHING, 0.0, 0.5);
    }

    /**
     * 지정한 레벨의 위력.
     *
     * @param level 1 이상
     */
    public double powerAt(int level) {
        int effective = Math.max(1, level);
        double result = base + coefficient * Math.pow(effective, exponent);
        return Double.isFinite(result) ? result : base;
    }
}
