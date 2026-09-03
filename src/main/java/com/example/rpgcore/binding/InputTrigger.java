package com.example.rpgcore.binding;

/**
 * 지시서 10장 [순간 입력].
 *
 * <p>키 조합의 뒤쪽 원소. 조합 = 유지 상태 1개 + 순간 입력 1개이며,
 * 가능한 조합 수는 2 x 7 = 14 이다.
 *
 * <p>[확인 필요 - 지시서 16장 4번]
 * {@link #JUMP} 는 Paper 점프 이벤트가 26.x 에 존재하는지 확인한 뒤에만
 * 쓴다. 존재하지 않으면 이 상수를 목록에서 빼고 조합 수를 12로 줄인다.
 *
 * <p>[확인 필요 - 지시서 16장 5번]
 * {@link #LEFT_CLICK} 의 허공 감지는 버전마다 이벤트 동작이 다를 수
 * 있으므로 실제 구동으로 확인한 뒤 확정한다.
 *
 * <p>실제 입력 감지는 4단계에서 구현한다.
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
    /** 점프 — 확인 후 사용 */
    JUMP,
    /** 핫바 슬롯 변경 */
    SLOT_CHANGE
}
