package com.example.rpgcore.binding;

import com.example.rpgcore.skill.SkillDefinition;
import com.example.rpgcore.util.Messages;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * 지시서 10장 [스킬 아이템 슬롯] — 스킬 아이템 만들기와 알아보기.
 *
 * <p>아이템에 스킬 id 를 심어 두고, 들고 우클릭하면 그 스킬이 나간다.
 *
 * <p>[확인 필요 - 지시서 16장]
 * {@code PersistentDataContainer} 와 {@code NamespacedKey} 가 26.x 에서
 * 유효한지 확인한다. 아이템에 값을 심고 읽는 곳은 이 클래스뿐이라
 * 다르면 여기만 고치면 된다.
 */
public final class SkillItems {

    /** 스킬 아이템에 쓸 아이템 종류. gui.yml 로 빼기 전까지의 기본값이다. */
    private static final Material DEFAULT_MATERIAL = Material.PAPER;

    private final NamespacedKey key;
    private final Messages messages;

    public SkillItems(Plugin plugin, Messages messages) {
        this.key = new NamespacedKey(plugin, "skill");
        this.messages = messages;
    }

    /** 스킬 아이템을 만든다. */
    public ItemStack create(SkillDefinition skill, int level) {
        ItemStack item = new ItemStack(DEFAULT_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(messages.format("skill.item.name",
                "skill", skill.display(), "level", level));
        meta.setLore(List.of(
                messages.format("skill.item.mana", "amount", Math.round(skill.manaAt(level))),
                messages.format("skill.item.cooldown", "seconds", skill.cooldownAt(level)),
                messages.format("skill.item.hint")));
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, skill.id());
        item.setItemMeta(meta);
        return item;
    }

    /** 아이템에 심어둔 스킬 id. 스킬 아이템이 아니면 null. */
    public String skillIdOf(ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public boolean isSkillItem(ItemStack item) {
        return skillIdOf(item) != null;
    }
}
