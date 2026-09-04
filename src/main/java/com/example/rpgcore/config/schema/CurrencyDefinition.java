package com.example.rpgcore.config.schema;

/**
 * 지시서 8장 [economy.yml] 의 특수 재화 하나.
 *
 * @param id      재화 id. 저장 파일의 currency 키와 같다
 * @param display 표시 이름
 * @param max     보유 상한. -1 이면 없음
 */
public record CurrencyDefinition(String id, String display, long max) {

    public boolean hasMax() {
        return max >= 0;
    }

    /** 상한을 넘지 않도록 자른다. */
    public long clamp(long amount) {
        long result = Math.max(0, amount);
        return hasMax() ? Math.min(result, max) : result;
    }
}
