package com.example.rpgcore.ui.gui;

import com.example.rpgcore.config.schema.GuiIcon;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 아이콘 아이템을 만든다.
 *
 * <p>[확인 필요 - 지시서 16장]
 * {@code ItemMeta#setDisplayName(String)} 과 {@code setLore(List)} 가
 * 26.x 에서 유효한지 확인한다. 아이템 표시 이름을 다루는 곳은
 * 이 클래스뿐이다.
 */
public final class Icons {

    /** gui.yml 의 material 을 읽지 못했을 때 쓰는 아이템. */
    private static final Material FALLBACK = Material.STONE;

    private Icons() {
    }

    /** 설정에 적힌 종류를 찾는다. 없으면 대체값. */
    public static Material material(GuiIcon icon) {
        if (icon == null || icon.material() == null) {
            return FALLBACK;
        }
        Material found = Material.matchMaterial(icon.material());
        return found == null ? FALLBACK : found;
    }

    /** 표시 이름과 설명이 붙은 아이템을 만든다. */
    public static ItemStack build(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
