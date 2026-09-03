package com.example.rpgcore.config.schema;

/**
 * gui.yml 의 아이콘 하나.
 *
 * @param slot     인벤토리 슬롯 번호
 * @param material 아이템 종류 이름. 알 수 없으면 화면이 대체값을 쓴다
 */
public record GuiIcon(int slot, String material) {
}
