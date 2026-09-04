package com.example.rpgcore.skill.cooldown;

import com.example.rpgcore.player.RpgPlayer;

/**
 * 지시서 3장 [skill/cooldown] — 스킬 쿨타임.
 *
 * <p>쿨타임 값은 {@code RpgPlayer} 가 들고 있고(지시서 4장), 이 서비스는
 * 읽고 쓰는 방법만 정한다. 저장 대상이 아니라 접속할 때마다 비어 있다.
 *
 * <p>시계는 {@link System#nanoTime()} 을 쓴다. 서버 시각이 바뀌어도
 * 쿨타임이 튀지 않는다.
 */
public final class CooldownService {

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    /** 쿨타임을 건다. 초가 0 이하면 걸지 않는다. */
    public void start(RpgPlayer rpgPlayer, String skillId, double seconds) {
        if (seconds <= 0) {
            rpgPlayer.cooldowns().remove(skillId);
            return;
        }
        long readyAt = System.nanoTime() + (long) (seconds * NANOS_PER_SECOND);
        rpgPlayer.cooldowns().put(skillId, readyAt);
    }

    /** 지금 쓸 수 있는지. */
    public boolean isReady(RpgPlayer rpgPlayer, String skillId) {
        return remainingSeconds(rpgPlayer, skillId) <= 0;
    }

    /** 남은 시간 (초). 다 됐으면 0. */
    public double remainingSeconds(RpgPlayer rpgPlayer, String skillId) {
        Long readyAt = rpgPlayer.cooldowns().get(skillId);
        if (readyAt == null) {
            return 0;
        }
        long remaining = readyAt - System.nanoTime();
        if (remaining <= 0) {
            rpgPlayer.cooldowns().remove(skillId);
            return 0;
        }
        return (double) remaining / NANOS_PER_SECOND;
    }

    /** 전부 지운다. */
    public void clear(RpgPlayer rpgPlayer) {
        rpgPlayer.cooldowns().clear();
    }
}
