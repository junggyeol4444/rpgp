package com.example.rpgcore.ui.bossbar;

import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.ui.HudChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

/**
 * 지시서 10장 — 보스바. 몬스터 체력을 띄우는 자리다.
 *
 * <p>커스텀 몬스터가 아직 없으므로 2단계에서는 띄우고 내리는 통로만
 * 만들어 둔다. 스스로 내용을 만들지 않으며, 부르는 쪽이 없으면
 * 아무 것도 보이지 않는다. (지시서 15장: 다음 단계 기능을 미리
 * 절반만 만들어두지 않는다)
 *
 * <p>[확인 필요 - 지시서 16장]
 * {@code Bukkit#createBossBar} 와 {@code BossBar} API 가 26.x 에서
 * 유효한지 확인한다. config.yml 의 ui.channels.bossbar 로 끌 수 있다.
 */
public final class BossBarChannel implements HudChannel {

    private final Map<UUID, BossBar> bars = new HashMap<>();

    @Override
    public String id() {
        return "bossbar";
    }

    @Override
    public void update(RpgPlayer rpgPlayer) {
        // 주기 갱신 대상이 아니다. show/hide 로만 움직인다.
    }

    /**
     * 보스바를 띄운다.
     *
     * @param progress 0.0 ~ 1.0
     */
    public void show(RpgPlayer rpgPlayer, String title, double progress) {
        BossBar bar = bars.get(rpgPlayer.uuid());
        if (bar == null) {
            bar = Bukkit.createBossBar(title, BarColor.RED, BarStyle.SOLID);
            bar.addPlayer(rpgPlayer.player());
            bars.put(rpgPlayer.uuid(), bar);
        } else {
            bar.setTitle(title);
        }
        bar.setProgress(Math.min(1.0, Math.max(0.0, progress)));
        bar.setVisible(true);
    }

    /** 보스바를 내린다. */
    public void hide(RpgPlayer rpgPlayer) {
        BossBar bar = bars.get(rpgPlayer.uuid());
        if (bar != null) {
            bar.setVisible(false);
        }
    }

    @Override
    public void detach(RpgPlayer rpgPlayer) {
        BossBar bar = bars.remove(rpgPlayer.uuid());
        if (bar != null) {
            bar.removeAll();
        }
    }
}
