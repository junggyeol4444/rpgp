package com.example.rpgcore.skill.mana;

import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.core.Lifecycle;
import com.example.rpgcore.core.MainThreadExecutor;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.stat.DerivedStat;

/**
 * 지시서 3장 [skill/mana] — 마나 자원.
 *
 * <p>기획서 6장 [자원]: 마나 + 쿨타임 병행.
 *
 * <p>마나는 저장 대상이 아니다. (지시서 7장 스키마에 없음)
 * 접속할 때 최대치로 시작하고 주기적으로 회복한다.
 */
public final class ManaService implements Lifecycle {

    /** 회복 주기. 1초. */
    private static final long REGEN_PERIOD_TICKS = 20;

    private final ConfigManager config;
    private final PlayerManager players;
    private final MainThreadExecutor mainThread;

    private MainThreadExecutor.Cancellable task;

    public ManaService(ConfigManager config, PlayerManager players,
                       MainThreadExecutor mainThread) {
        this.config = config;
        this.players = players;
        this.mainThread = mainThread;
    }

    @Override
    public String serviceName() {
        return "ManaService";
    }

    @Override
    public void enable() {
        task = mainThread.runTimer(this::regenAll, REGEN_PERIOD_TICKS);
    }

    @Override
    public void disable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** 최대 마나. 파생 수치에서 읽는다. */
    public double max(RpgPlayer rpgPlayer) {
        return Math.max(0, rpgPlayer.derived().get(DerivedStat.MAX_MANA));
    }

    /** 마나가 충분한지. */
    public boolean has(RpgPlayer rpgPlayer, double cost) {
        return rpgPlayer.mana() >= cost;
    }

    /**
     * 마나를 쓴다.
     *
     * @return 모자라서 쓰지 못했으면 false
     */
    public boolean consume(RpgPlayer rpgPlayer, double cost) {
        if (cost <= 0) {
            return true;
        }
        if (!has(rpgPlayer, cost)) {
            return false;
        }
        rpgPlayer.mana(rpgPlayer.mana() - cost);
        return true;
    }

    /** 최대치까지 채운다. */
    public void fill(RpgPlayer rpgPlayer) {
        rpgPlayer.mana(max(rpgPlayer));
    }

    private void regenAll() {
        double perSecond = config.skill().manaRegenPerSecond();
        if (perSecond <= 0) {
            return;
        }
        for (RpgPlayer rpgPlayer : players.onlinePlayers()) {
            if (!rpgPlayer.isOnline()) {
                continue;
            }
            double max = max(rpgPlayer);
            if (rpgPlayer.mana() < max) {
                rpgPlayer.mana(Math.min(max, rpgPlayer.mana() + perSecond));
            }
        }
    }
}
