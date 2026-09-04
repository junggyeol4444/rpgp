package com.example.rpgcore.ui.gui;

import com.example.rpgcore.config.schema.GuiScreen;
import com.example.rpgcore.player.RpgPlayer;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 지시서 13장 [공통] — 인벤토리 기반 GUI 한 화면.
 *
 * <p>클릭 이벤트는 전부 취소하고 자체 처리한다. 이 클래스를
 * {@link InventoryHolder} 로 두면 제목 문자열을 비교하지 않고도
 * 우리 화면인지 알 수 있다.
 *
 * <p>[확인 완료] {@code Bukkit#createInventory(InventoryHolder, int, String)}
 * 는 26.1.2 에 있다. 컴포넌트를 받는 쪽으로 대체하라는 사용 중단 표시가
 * 붙어 있고, 제거 예정 표시는 없다. Paper 26.1.2 API 소스로 컴파일해 확인했다. (tools/verify-against-paper.sh)
 * 인벤토리를 만드는 곳은 이 클래스 한 줄뿐이다.
 */
public abstract class Gui implements InventoryHolder {

    private final GuiScreen screen;

    /**
     * 처음 필요할 때 만든다. 생성자에서 만들면 하위 클래스가 아직 다 세워지지
     * 않은 상태로 this 가 밖으로 나간다.
     */
    private Inventory inventory;

    protected Gui(GuiScreen screen) {
        this.screen = screen;
    }

    @Override
    public final Inventory getInventory() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, screen.size(), screen.title());
        }
        return inventory;
    }

    public final GuiScreen screen() {
        return screen;
    }

    /** 화면 내용을 채운다. 열 때와 값이 바뀔 때 불린다. */
    public abstract void render(RpgPlayer rpgPlayer);

    /** 클릭 처리. 이벤트는 이미 취소된 상태로 들어온다. */
    public abstract void onClick(RpgPlayer rpgPlayer, int slot, InventoryClickEvent event);

    /** 닫을 때 정리할 것이 있으면. */
    public void onClose(RpgPlayer rpgPlayer) {
    }
}
