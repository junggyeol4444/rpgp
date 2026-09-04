package com.example.rpgcore.player;

import com.example.rpgcore.binding.InputState;
import com.example.rpgcore.player.data.PlayerData;
import com.example.rpgcore.stat.DerivedStats;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

/**
 * 지시서 4장 [RpgPlayer] — 접속 중에만 존재하는 런타임 상태.
 *
 * <p>지시서 15장에 따라 아직 오지 않은 단계의 항목은 두지 않는다.
 */
public final class RpgPlayer {

    private final Player player;
    private final PlayerData data;

    /**
     * 파생 수치 캐시. 스탯·직업·스킬이 바뀔 때만 다시 만든다.
     * 데미지 이벤트는 호출 빈도가 높아 매번 계산하면 안 된다. (지시서 11장)
     */
    private volatile DerivedStats derived = DerivedStats.empty();

    /** 내부 HP. 바닐라 하트 20칸과 별개다. (지시서 9장) */
    private double health;

    /** 현재 마나. */
    private double mana;

    /** 웅크림·달리기 유지 상태. (지시서 10장) */
    private final InputState inputState = new InputState();

    /**
     * 스킬 id -> 쿨타임이 끝나는 시각 (nanoTime 기준).
     * 저장 대상이 아니다. 입력 처리와 주기 갱신이 서로 다른 시점에
     * 건드릴 수 있어 동시성 맵을 쓴다.
     */
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public RpgPlayer(Player player, PlayerData data) {
        this.player = player;
        this.data = data;
    }

    public Player player() {
        return player;
    }

    public PlayerData data() {
        return data;
    }

    public UUID uuid() {
        return data.uuid();
    }

    public boolean isOnline() {
        return player.isOnline();
    }

    public DerivedStats derived() {
        return derived;
    }

    public void derived(DerivedStats derived) {
        this.derived = derived;
    }

    public double health() {
        return health;
    }

    public void health(double health) {
        this.health = health;
    }

    public double mana() {
        return mana;
    }

    public void mana(double mana) {
        this.mana = mana;
    }

    public InputState inputState() {
        return inputState;
    }

    public Map<String, Long> cooldowns() {
        return cooldowns;
    }
}
