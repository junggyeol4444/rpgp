package com.example.rpgcore.ui.tab;

import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.ui.HudChannel;
import com.example.rpgcore.util.Messages;

/**
 * 지시서 10장 — 탭 목록에 레벨 표시.
 *
 * <p>[확인 완료] {@code Player#setPlayerListHeaderFooter(String, String)} 는
 * 26.1.2 에 있다. {@code sendPlayerListHeaderAndFooter(Component, Component)}
 * 로 대체하라는 사용 중단 표시가 붙어 있고, 제거 예정 표시는 없다.
 * Paper 26.1.2 API 소스로 컴파일해 확인했다. (tools/verify-against-paper.sh)
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
