package com.example.rpgcore.binding;

import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.core.Lifecycle;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.player.data.PlayerData;
import com.example.rpgcore.skill.SkillDefinition;
import com.example.rpgcore.skill.SkillService;
import com.example.rpgcore.storage.dirty.SavePriority;
import com.example.rpgcore.storage.dirty.SaveScheduler;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * 지시서 3장 [binding/BindingService] — 스킬 등록·해제.
 *
 * <p>기획서 6장 [발동 방식]: 스킬 아이템과 키 조합을 함께 쓰고,
 * 어느 스킬을 어느 쪽에 태울지는 플레이어가 정한다.
 *
 * <p>바인딩 변경은 지연 저장 대상이다. (지시서 5장 [저장 정책])
 */
public final class BindingService implements Lifecycle {

    /** 등록 시도의 결과. */
    public enum Result {
        OK,
        /** 그런 스킬이 없음 */
        UNKNOWN_SKILL,
        /** 해금하지 않은 스킬 */
        NOT_UNLOCKED,
        /** 슬롯 번호가 열려 있는 칸을 벗어남 */
        SLOT_OUT_OF_RANGE,
        /** 이미 쓰이고 있는 키 조합 */
        COMBO_TAKEN,
        /** 쓸 수 없는 입력 (미확인 이벤트 등) */
        TRIGGER_UNAVAILABLE
    }

    /**
     * 지금 쓸 수 있는 순간 입력.
     *
     * <p>지시서 16장 4번: Paper 점프 이벤트의 26.x 존재 여부가 확인되지
     * 않았다. 확인 전까지 {@link InputTrigger#JUMP} 를 빼고 조합 수를
     * 14 대신 12로 둔다. (지시서 10장 [구현 메모])
     */
    private static final Set<InputTrigger> AVAILABLE_TRIGGERS =
            EnumSet.complementOf(EnumSet.of(InputTrigger.JUMP));

    private final ConfigManager config;
    private final SaveScheduler saves;
    private final SkillService skills;
    private final SkillItems items;

    public BindingService(ConfigManager config, SaveScheduler saves,
                          SkillService skills, SkillItems items) {
        this.config = config;
        this.saves = saves;
        this.skills = skills;
        this.items = items;
    }

    @Override
    public String serviceName() {
        return "BindingService";
    }

    /** 지금 쓸 수 있는 순간 입력 목록. */
    public Set<InputTrigger> availableTriggers() {
        return AVAILABLE_TRIGGERS;
    }

    /** 가능한 조합 가짓수. 유지 상태 2종 x 쓸 수 있는 순간 입력. */
    public int comboCapacity() {
        return HoldState.values().length * AVAILABLE_TRIGGERS.size();
    }

    /** 지금 열려 있는 아이템 슬롯 수. */
    public int slotCount(PlayerData data) {
        return config.skill().slotCount(data.job().stage());
    }

    // ------------------------------------------------------------
    // 아이템 슬롯
    // ------------------------------------------------------------

    /** 슬롯에 등록된 스킬 id. 비었으면 null. */
    public String slotSkill(PlayerData data, int index) {
        List<String> slots = data.binding().itemSlots();
        return index >= 0 && index < slots.size() ? slots.get(index) : null;
    }

    /** 슬롯에 스킬을 등록하고 스킬 아이템을 준다. */
    public Result setSlot(RpgPlayer rpgPlayer, int index, String skillId) {
        PlayerData data = rpgPlayer.data();
        if (index < 0 || index >= slotCount(data)) {
            return Result.SLOT_OUT_OF_RANGE;
        }
        SkillDefinition skill = skills.tree().get(skillId);
        if (skill == null) {
            return Result.UNKNOWN_SKILL;
        }
        if (!data.skill().unlocked().contains(skillId)) {
            return Result.NOT_UNLOCKED;
        }

        ensureSlotSize(data, slotCount(data));
        String previous = data.binding().itemSlots().get(index);
        if (previous != null) {
            removeSkillItem(rpgPlayer, previous);
        }
        data.binding().itemSlots().set(index, skillId);
        rpgPlayer.player().getInventory()
                .addItem(items.create(skill, data.skill().levelOf(skillId)));
        saves.markDirty(data, SavePriority.DEFERRED);
        return Result.OK;
    }

    /** 슬롯을 비우고 해당 스킬 아이템을 거둔다. */
    public void clearSlot(RpgPlayer rpgPlayer, int index) {
        PlayerData data = rpgPlayer.data();
        ensureSlotSize(data, slotCount(data));
        if (index < 0 || index >= data.binding().itemSlots().size()) {
            return;
        }
        String previous = data.binding().itemSlots().get(index);
        if (previous != null) {
            removeSkillItem(rpgPlayer, previous);
        }
        data.binding().itemSlots().set(index, null);
        saves.markDirty(data, SavePriority.DEFERRED);
    }

    /** 저장된 슬롯 목록을 지금 열린 칸 수에 맞춘다. */
    private void ensureSlotSize(PlayerData data, int size) {
        List<String> slots = data.binding().itemSlots();
        while (slots.size() < size) {
            slots.add(null);
        }
        while (slots.size() > size) {
            slots.remove(slots.size() - 1);
        }
    }

    private void removeSkillItem(RpgPlayer rpgPlayer, String skillId) {
        Inventory inventory = rpgPlayer.player().getInventory();
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (skillId.equals(items.skillIdOf(contents[i]))) {
                inventory.setItem(i, null);
            }
        }
    }

    // ------------------------------------------------------------
    // 키 조합
    // ------------------------------------------------------------

    /** 조합에 걸린 스킬 id. 없으면 null. */
    public String comboSkill(PlayerData data, HoldState hold, InputTrigger trigger) {
        for (PlayerData.KeyCombo combo : data.binding().keyCombos()) {
            if (combo.hold() == hold && combo.trigger() == trigger) {
                return combo.skillId();
            }
        }
        return null;
    }

    /**
     * 키 조합을 등록한다.
     *
     * <p>지시서 13장: 이미 쓰인 조합은 중복 등록을 막는다.
     */
    public Result setCombo(RpgPlayer rpgPlayer, HoldState hold, InputTrigger trigger,
                           String skillId) {
        if (!AVAILABLE_TRIGGERS.contains(trigger)) {
            return Result.TRIGGER_UNAVAILABLE;
        }
        PlayerData data = rpgPlayer.data();
        if (skills.tree().get(skillId) == null) {
            return Result.UNKNOWN_SKILL;
        }
        if (!data.skill().unlocked().contains(skillId)) {
            return Result.NOT_UNLOCKED;
        }
        if (comboSkill(data, hold, trigger) != null) {
            return Result.COMBO_TAKEN;
        }
        data.binding().keyCombos().add(new PlayerData.KeyCombo(hold, trigger, skillId));
        saves.markDirty(data, SavePriority.DEFERRED);
        return Result.OK;
    }

    /** 조합 등록을 지운다. */
    public void clearCombo(RpgPlayer rpgPlayer, HoldState hold, InputTrigger trigger) {
        boolean removed = rpgPlayer.data().binding().keyCombos()
                .removeIf(combo -> combo.hold() == hold && combo.trigger() == trigger);
        if (removed) {
            saves.markDirty(rpgPlayer.data(), SavePriority.DEFERRED);
        }
    }

    /** 슬롯과 조합을 전부 비운다. (/rpg admin bindreset) */
    public void resetAll(RpgPlayer rpgPlayer) {
        PlayerData data = rpgPlayer.data();
        for (String skillId : new ArrayList<>(data.binding().itemSlots())) {
            if (skillId != null) {
                removeSkillItem(rpgPlayer, skillId);
            }
        }
        data.binding().itemSlots().clear();
        data.binding().keyCombos().clear();
        saves.markDirty(data, SavePriority.DEFERRED);
    }
}
