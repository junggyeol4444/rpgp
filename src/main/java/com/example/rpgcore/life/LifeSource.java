package com.example.rpgcore.life;

/**
 * 지시서 8장 [life.yml] 의 sources 키.
 *
 * <p>기획서 3장 [생활 계열 경험치] 의 획득 경로다.
 */
public enum LifeSource {

    /** 블록 파괴. 벌목 · 농사 · 채광 */
    BLOCK_BREAK,
    /** 낚시 */
    FISHING,
    /** 조리 (화로에서 결과물 수령) */
    COOKING,
    /** 조합대 제작 */
    CRAFT,
    /** 양조 (양조대에서 결과물 수령) */
    BREW;

    /** 이름으로 찾는다. 없으면 null. */
    public static LifeSource fromConfig(String value) {
        if (value == null) {
            return null;
        }
        for (LifeSource source : values()) {
            if (source.name().equalsIgnoreCase(value)) {
                return source;
            }
        }
        return null;
    }
}
