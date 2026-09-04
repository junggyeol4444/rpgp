package com.example.rpgcore.binding;

/**
 * 지시서 3장 [binding/InputState] — 웅크림·달리기 유지 상태 추적.
 *
 * <p>지시서 10장 [전제]: 서버는 클라이언트 키보드를 읽을 수 없다.
 * 토글 이벤트로만 상태를 알 수 있어서 플레이어별로 들고 있는다.
 *
 * <p>두 상태는 동시에 성립하지 않는다. 둘 다 켜진 것으로 들어오면
 * 웅크림을 우선한다. 바닐라에서 웅크리면 달리기가 풀리기 때문이다.
 */
public final class InputState {

    private volatile boolean sneaking;
    private volatile boolean sprinting;

    public void sneaking(boolean sneaking) {
        this.sneaking = sneaking;
    }

    public void sprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    public boolean sneaking() {
        return sneaking;
    }

    public boolean sprinting() {
        return sprinting;
    }

    /** 지금 성립하는 유지 상태. 없으면 null. */
    public HoldState active() {
        if (sneaking) {
            return HoldState.SNEAK;
        }
        return sprinting ? HoldState.SPRINT : null;
    }

    /** 퇴장·부활 등에서 초기화. */
    public void clear() {
        sneaking = false;
        sprinting = false;
    }
}
