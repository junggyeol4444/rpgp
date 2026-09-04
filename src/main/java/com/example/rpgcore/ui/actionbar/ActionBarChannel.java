package com.example.rpgcore.ui.actionbar;

import com.example.rpgcore.combat.HealthService;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.stat.DerivedStat;
import com.example.rpgcore.ui.HudChannel;
import com.example.rpgcore.util.Messages;
import org.bukkit.entity.Player;

/**
 * 지시서 10장 — 액션바에 HP · 마나.
 *
 * <p>[확인 완료] {@code Player#sendActionBar(String)} 는 26.1.2 에 있다.
 * 다만 {@code sendActionBar(Component)} 로 대체하라는 사용 중단 표시가
 * 붙어 있다. 제거 예정 표시는 없다. Paper 26.1.2 API 소스로 컴파일해 확인했다. (tools/verify-against-paper.sh)
 * 액션바를 보내는 곳은 이 파일 한 줄뿐이라 옮길 때도 여기만 고친다.
 */
public final class ActionBarChannel implements HudChannel {

    private final Messages messages;
    private final HealthService health;

    public ActionBarChannel(Messages messages, HealthService health) {
        this.messages = messages;
        this.health = health;
    }

    @Override
    public String id() {
        return "actionbar";
    }

    @Override
    public void update(RpgPlayer rpgPlayer) {
        Player player = rpgPlayer.player();
        String text = messages.format("hud.actionbar",
                "hp", round(rpgPlayer.health()),
                "maxHp", round(health.maxHealth(rpgPlayer)),
                "mana", round(rpgPlayer.mana()),
                "maxMana", round(rpgPlayer.derived().get(DerivedStat.MAX_MANA)));
        player.sendActionBar(text);
    }

    private static long round(double value) {
        return Math.round(value);
    }
}
