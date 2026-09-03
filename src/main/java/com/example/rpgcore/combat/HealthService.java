package com.example.rpgcore.combat;

import com.example.rpgcore.core.Lifecycle;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.stat.DerivedStat;
import org.bukkit.entity.Player;

/**
 * 지시서 9장 [내부 HP] — 내부 HP 와 하트 20칸의 환산.
 *
 * <p>바닐라 최대 체력은 20으로 고정하고, 실제 HP 는 이 서비스가 들고 있다.
 * 표시할 때만 20칸 비율로 환산한다. 최대 체력 속성을 건드리지 않으므로
 * 다른 플러그인·바닐라 로직과 부딪히지 않는다.
 *
 * <p>지시서 9장에 따라 바닐라 회복·데미지가 내부 HP 와 어긋나지 않도록
 * 동기화 지점을 이 클래스 하나로 모은다. 다른 곳에서 setHealth 를
 * 부르지 않는다.
 *
 * <p>[확인 필요 - 지시서 16장]
 * {@code Player#setHealth(double)} 가 26.x 에서 유효한지 확인한다.
 * 플러그인에서 이 API 를 부르는 곳은 이 클래스뿐이다.
 *
 * <p>내부 HP 는 저장 대상이 아니다. (지시서 7장 스키마에 없음)
 * 접속할 때마다 최대치로 시작한다.
 */
public final class HealthService implements Lifecycle {

    /** 바닐라 하트 칸 수. 고정값이다. */
    private static final double VANILLA_MAX = 20.0;

    /** HP 가 남아 있는데 하트가 0칸으로 보이지 않도록 두는 최소 표시량. */
    private static final double MIN_VISIBLE = 0.5;

    @Override
    public String serviceName() {
        return "HealthService";
    }

    /** 최대 내부 HP. 파생 수치에서 읽는다. 0 이하면 1로 본다. */
    public double maxHealth(RpgPlayer rpgPlayer) {
        double max = rpgPlayer.derived().get(DerivedStat.MAX_HEALTH);
        return max > 0 ? max : 1.0;
    }

    /** 접속 직후. 최대치로 채우고 표시를 맞춘다. */
    public void initialize(RpgPlayer rpgPlayer) {
        rpgPlayer.health(maxHealth(rpgPlayer));
        rpgPlayer.mana(rpgPlayer.derived().get(DerivedStat.MAX_MANA));
        syncDisplay(rpgPlayer);
    }

    /**
     * 파생 수치가 바뀐 뒤. 최대치가 줄었으면 현재 HP 를 깎고,
     * 늘었으면 비율을 유지한다.
     */
    public void onStatsChanged(RpgPlayer rpgPlayer) {
        double max = maxHealth(rpgPlayer);
        if (rpgPlayer.health() <= 0) {
            // 죽어 있는 상태는 건드리지 않는다.
            return;
        }
        rpgPlayer.health(Math.min(rpgPlayer.health(), max));
        rpgPlayer.mana(Math.min(rpgPlayer.mana(), rpgPlayer.derived().get(DerivedStat.MAX_MANA)));
        syncDisplay(rpgPlayer);
    }

    /**
     * 내부 HP 를 깎는다.
     *
     * @return 남은 HP. 0 이면 죽는다
     */
    public double damage(RpgPlayer rpgPlayer, double amount) {
        if (amount <= 0 || !Double.isFinite(amount)) {
            return rpgPlayer.health();
        }
        double next = rpgPlayer.health() - amount;
        if (next <= 0) {
            rpgPlayer.health(0);
            kill(rpgPlayer);
            return 0;
        }
        rpgPlayer.health(next);
        syncDisplay(rpgPlayer);
        return next;
    }

    /** 내부 HP 를 채운다. 최대치를 넘지 않는다. */
    public double heal(RpgPlayer rpgPlayer, double amount) {
        if (amount <= 0 || !Double.isFinite(amount) || rpgPlayer.health() <= 0) {
            return rpgPlayer.health();
        }
        double next = Math.min(rpgPlayer.health() + amount, maxHealth(rpgPlayer));
        rpgPlayer.health(next);
        syncDisplay(rpgPlayer);
        return next;
    }

    /** 부활 처리. 최대치로 되돌린다. */
    public void restore(RpgPlayer rpgPlayer) {
        rpgPlayer.health(maxHealth(rpgPlayer));
        rpgPlayer.mana(rpgPlayer.derived().get(DerivedStat.MAX_MANA));
        syncDisplay(rpgPlayer);
    }

    /** 내부 HP 비율을 하트 20칸으로 옮긴다. */
    public void syncDisplay(RpgPlayer rpgPlayer) {
        Player player = rpgPlayer.player();
        if (!player.isOnline()) {
            return;
        }
        player.setHealth(displayHealth(rpgPlayer));
    }

    /** 표시할 하트 값. 0.0 ~ 20.0 */
    public double displayHealth(RpgPlayer rpgPlayer) {
        double health = rpgPlayer.health();
        if (health <= 0) {
            return 0.0;
        }
        double ratio = health / maxHealth(rpgPlayer);
        double shown = ratio * VANILLA_MAX;
        return Math.min(VANILLA_MAX, Math.max(MIN_VISIBLE, shown));
    }

    private void kill(RpgPlayer rpgPlayer) {
        Player player = rpgPlayer.player();
        if (player.isOnline()) {
            // 하트를 0으로 만들면 바닐라 사망 처리가 그대로 돈다.
            player.setHealth(0.0);
        }
    }
}
