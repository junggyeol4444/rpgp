package com.example.rpgcore.player;

import com.example.rpgcore.player.data.PlayerData;
import java.util.UUID;
import org.bukkit.entity.Player;

/**
 * 지시서 4장 [RpgPlayer] — 접속 중에만 존재하는 런타임 상태.
 *
 * <p>1단계에서 들고 있는 것은 Player 참조와 {@link PlayerData} 뿐이다.
 * 지시서 15장(다음 단계 기능을 미리 절반만 만들어두지 않는다)에 따라
 * 아래 항목은 해당 단계에서 추가한다.
 *
 * <ul>
 *   <li>DerivedStats 캐시, 내부 HP — 2단계</li>
 *   <li>현재 마나, 쿨타임 맵 — 4단계</li>
 *   <li>InputState (웅크림/달리기 유지 여부) — 4단계</li>
 * </ul>
 */
public final class RpgPlayer {

    private final Player player;
    private final PlayerData data;

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

    /** 아직 접속 중인지. 퇴장한 뒤에도 참조가 남아 있을 수 있다. */
    public boolean isOnline() {
        return player.isOnline();
    }
}
