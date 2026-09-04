package com.example.rpgcore.config.schema;

/**
 * 지시서 8장 [stats.yml] 의 reset 블록.
 * 기획서 4장: 스탯 초기화는 유료·제한적으로만 허용한다.
 *
 * @param allowed    허용 여부
 * @param currencyId 소모할 특수 재화 id
 * @param amount     첫 초기화 비용
 * @param scaling    초기화 횟수마다 붙는 비용 배수
 */
public record ResetSettings(boolean allowed, String currencyId, long amount, double scaling) {

    public static ResetSettings defaults() {
        return new ResetSettings(true, "dungeon_coin", 50, 1.5);
    }

    /**
     * 이번 초기화 비용.
     *
     * @param previousResets 지금까지 초기화한 횟수
     */
    public long costFor(int previousResets) {
        double cost = amount * Math.pow(Math.max(1.0, scaling), Math.max(0, previousResets));
        if (cost >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return (long) Math.ceil(cost);
    }
}
