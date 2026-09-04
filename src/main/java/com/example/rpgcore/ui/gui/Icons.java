package com.example.rpgcore.ui.gui;

import com.example.rpgcore.config.schema.GuiIcon;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 아이콘 아이템을 만든다.
 *
 * <p>[확인 완료] {@code ItemMeta#setDisplayName(String)} 과
 * {@code setLore(List)} 는 26.1.2 에 있다. 각각 {@code displayName(Component)}
 * 와 {@code lore(List)} 로 대체하라는 사용 중단 표시가 붙어 있고,
 * 제거 예정 표시는 없다. Paper 26.1.2 API 소스로 컴파일해 확인했다. (tools/verify-against-paper.sh)
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
