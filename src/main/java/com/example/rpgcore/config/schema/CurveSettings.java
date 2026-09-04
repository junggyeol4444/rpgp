package com.example.rpgcore.config.schema;

/**
 * 지시서 8장 [levels.yml] 의 curve 블록.
 *
 * @param type   곡선 종류. 현재 EXPONENTIAL 만 구현되어 있다.
 * @param base   1레벨에서 2레벨로 갈 때 요구치
 * @param factor 레벨당 배수
 */
public record CurveSettings(String type, double base, double factor) {

    public static final String EXPONENTIAL = "EXPONENTIAL";

    /** levels.yml 이 통째로 잘못됐을 때 쓰는 기본값. */
    public static CurveSettings defaultCombat() {
        return new CurveSettings(EXPONENTIAL, 100.0, 1.12);
    }

    /** 생활 트랙 기본값. */
    public static CurveSettings defaultLife() {
        return new CurveSettings(EXPONENTIAL, 50.0, 1.10);
    }
}
