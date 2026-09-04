package com.example.rpgcore.quest.repeat;

/**
 * 지시서 3장 [quest/repeat] — 일일 · 주간 리셋.
 *
 * <p>기준 시각을 정하는 대신 "마지막 리셋으로부터 얼마나 지났는지"로
 * 판단한다. 서버 시간대를 가정하지 않기 위해서다.
 * 기획서에 리셋 기준 시각이 적혀 있지 않다.
 */
public enum ResetCycle {

    DAILY(24L * 60 * 60 * 1000),
    WEEKLY(7L * 24 * 60 * 60 * 1000);

    private final long periodMillis;

    ResetCycle(long periodMillis) {
        this.periodMillis = periodMillis;
    }

    public long periodMillis() {
        return periodMillis;
    }

    /**
     * 리셋할 때가 됐는지.
     *
     * @param lastResetAt 마지막 리셋 시각. 0 이면 아직 한 번도 안 함
     * @param now         지금
     */
    public boolean isDue(long lastResetAt, long now) {
        return lastResetAt <= 0 || now - lastResetAt >= periodMillis;
    }

    /** 다음 리셋까지 남은 시간 (밀리초). 이미 지났으면 0. */
    public long remaining(long lastResetAt, long now) {
        if (isDue(lastResetAt, now)) {
            return 0;
        }
        return periodMillis - (now - lastResetAt);
    }
}
