package com.example.rpgcore.core;

/**
 * 지시서 3장.
 *
 * <p>enable/disable 순서 관리 대상. 등록 순서대로 enable 하고
 * 역순으로 disable 한다.
 */
public interface Lifecycle {

    /** 로그에 쓸 이름. */
    String serviceName();

    /** 서버 기동 시. 등록 순서대로 호출된다. */
    default void enable() {
    }

    /** 서버 종료 시. 등록 역순으로 호출된다. */
    default void disable() {
    }
}
