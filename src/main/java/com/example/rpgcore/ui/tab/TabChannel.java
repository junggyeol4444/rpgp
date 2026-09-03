package com.example.rpgcore.ui.tab;

import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.ui.HudChannel;
import com.example.rpgcore.util.Messages;

/**
 * 지시서 10장 — 탭 목록에 레벨 표시.
 *
 * <p>[확인 필요 - 지시서 16장]
 * {@code Player#setPlayerListHeaderFooter(String, String)} 가 26.x 에서
 * 유효한지 확인한다. config.yml 의 ui.channels.tab 으로 끌 수 있다.
 */
public final class TabChannel implements HudChannel {

    private final Messages messages;

    public TabChannel(Messages messages) {
        this.messages = messages;
    }

    @Override
    public String id() {
        return "tab";
    }

    @Override
    public void update(RpgPlayer rpgPlayer) {
        int level = rpgPlayer.data().combat().level();
        rpgPlayer.player().setPlayerListHeaderFooter(
                messages.format("hud.tab.header", "level", level),
                messages.format("hud.tab.footer", "level", level));
    }
}
