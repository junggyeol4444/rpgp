package com.example.rpgcore.binding;

/**
 * 지시서 10장 [순간 입력].
 *
 * <p>키 조합의 뒤쪽 원소. 조합 = 유지 상태 1개 + 순간 입력 1개이며,
 * 가능한 조합 수는 2 x 7 = 14 이다.
 *
 * <p>[지시서 16장 4번 확인 완료]
 * Paper 26.1.2 에 {@code com.destroystokyo.paper.event.player.PlayerJumpEvent}
 * 가 있다. 따라서 {@link #JUMP} 를 그대로 쓰고 조합 수는 14다.
 *
 * <p>[지시서 16장 5번 일부 확인]
 * {@code Action.LEFT_CLICK_AIR} 상수는 26.1.2 에 있다.
 * 다만 허공 좌클릭이 실제로 매번 서버까지 오는지는 구동해 봐야 안다.
 */
public enum InputTrigger {
    /** 좌클릭 */
    LEFT_CLICK,
    /** 우클릭 */
    RIGHT_CLICK,
    /** 손 바꾸기 (F) */
    SWAP_HAND,
    /** 아이템 버리기 (Q) */
    DROP_ITEM,
    /** 인벤토리 열기 (E) */
    OPEN_INVENTORY,
    /** 점프 */
    JUMP,
    /** 핫바 슬롯 변경 */
    SLOT_CHANGE
}
