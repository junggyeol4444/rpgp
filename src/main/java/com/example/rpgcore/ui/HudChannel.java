package com.example.rpgcore.ui;

import com.example.rpgcore.player.RpgPlayer;

/**
 * 상시 표시 요소 하나. (지시서 10장 [상시 표시])
 *
 * <p>지시서 0장 5번에 따라, 확인되지 않은 표시 API 는 전부 구현체
 * 한 파일 안에만 둔다. API 가 26.x 에서 다르면 그 파일만 고치면 되고,
 * 최악의 경우 config.yml 에서 해당 채널만 끄면 된다.
 */
public interface HudChannel {

    /** config.yml 의 ui 블록에서 쓰는 이름. */
    String id();

    /** 접속 직후 한 번. */
    default void attach(RpgPlayer rpgPlayer) {
    }

    /** 주기적으로. */
    void update(RpgPlayer rpgPlayer);

    /** 퇴장 직전 정리. */
    default void detach(RpgPlayer rpgPlayer) {
    }
}
