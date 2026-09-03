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
 * <p>[확인 필요 - 지시서 16장]
 * {@code Player#sendActionBar(String)} 가 26.x 에서 유효한지 확인한다.
 * 액션바를 보내는 곳은 이 파일 한 줄뿐이라, 다르면 여기만 고친다.
 * 그때까지는 config.yml 의 ui.channels.actionbar 로 끌 수 있다.
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
