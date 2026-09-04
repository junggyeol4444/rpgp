package com.example.rpgcore.binding;

/**
 * 지시서 10장 [유지 상태].
 *
 * <p>키 조합의 앞쪽 원소. 2종이며 동시에 성립하지 않는다.
 * 서버는 클라이언트 키보드를 읽을 수 없으므로 서버로 전달되는
 * 토글 이벤트로만 추적한다.
 */
public enum HoldState {
    /** 웅크림 */
    SNEAK,
    /** 달리기 */
    SPRINT
}
