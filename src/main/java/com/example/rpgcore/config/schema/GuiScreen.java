package com.example.rpgcore.config.schema;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 지시서 13장 — gui.yml 의 화면 하나.
 *
 * <p>제목·크기·아이콘 위치를 전부 설정에서 읽는다.
 *
 * @param id    화면 id
 * @param title 창 제목
 * @param rows  줄 수 (1~6)
 * @param icons 역할 이름 -> 아이콘
 */
public record GuiScreen(String id, String title, int rows, Map<String, GuiIcon> icons) {

    public GuiScreen {
        rows = Math.min(6, Math.max(1, rows));
        icons = new LinkedHashMap<>(icons);
    }

    public static GuiScreen fallback(String id) {
        return new GuiScreen(id, id, 3, Map.of());
    }

    public int size() {
        return rows * 9;
    }

    /** 없으면 null. */
    public GuiIcon icon(String role) {
        return icons.get(role);
    }
}
