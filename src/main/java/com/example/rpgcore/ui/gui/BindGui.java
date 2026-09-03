package com.example.rpgcore.ui.gui;

import com.example.rpgcore.binding.BindingService;
import com.example.rpgcore.binding.HoldState;
import com.example.rpgcore.binding.InputTrigger;
import com.example.rpgcore.config.schema.GuiScreen;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.player.data.PlayerData;
import com.example.rpgcore.skill.SkillDefinition;
import com.example.rpgcore.skill.SkillService;
import com.example.rpgcore.util.Messages;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * 지시서 13장 5) 스킬 등록 창.
 *
 * <p>위쪽 줄은 스킬 아이템 슬롯, 아래쪽은 키 조합 칸이다.
 * 칸을 누르면 해금한 스킬을 차례로 돌려 가며 걸고, 우클릭하면 비운다.
 * 화면을 여러 번 넘기지 않고 한 창에서 끝내기 위한 방식이다.
 *
 * <p>조합 칸은 유지 상태 2종 x 쓸 수 있는 순간 입력만큼 놓인다.
 * 점프 이벤트가 확인되지 않아 지금은 12칸이다. (지시서 16장 4번)
 */
public final class BindGui extends Gui {

    private static final String ROLE_SLOT = "slot";
    private static final String ROLE_EMPTY = "empty";
    private static final String ROLE_COMBO = "combo";
    private static final String ROLE_INFO = "info";

    /** 아이템 슬롯을 놓는 줄. */
    private static final int SLOT_ROW = 0;
    /** 조합 칸을 놓기 시작하는 줄. */
    private static final int COMBO_ROW = 1;

    private final BindingService bindings;
    private final SkillService skills;
    private final Messages messages;
    private final GuiManager guis;

    private final Map<Integer, Integer> slotToIndex = new HashMap<>();
    private final Map<Integer, PlayerData.KeyCombo> slotToCombo = new HashMap<>();

    public BindGui(GuiScreen screen, BindingService bindings, SkillService skills,
                   Messages messages, GuiManager guis) {
        super(screen);
        this.bindings = bindings;
        this.skills = skills;
        this.messages = messages;
        this.guis = guis;
    }

    @Override
    public void render(RpgPlayer rpgPlayer) {
        Inventory inventory = getInventory();
        inventory.clear();
        slotToIndex.clear();
        slotToCombo.clear();

        PlayerData data = rpgPlayer.data();
        renderInfo(inventory, data);
        renderItemSlots(inventory, data);
        renderCombos(inventory, data);
    }

    private void renderInfo(Inventory inventory, PlayerData data) {
        var icon = screen().icon(ROLE_INFO);
        if (icon == null || icon.slot() < 0 || icon.slot() >= screen().size()) {
            return;
        }
        inventory.setItem(icon.slot(), Icons.build(Icons.material(icon),
                messages.format("gui.bind.info.name"),
                List.of(messages.format("gui.bind.info.slots",
                                "count", bindings.slotCount(data)),
                        messages.format("gui.bind.info.combos",
                                "used", data.binding().keyCombos().size(),
                                "max", bindings.comboCapacity()),
                        messages.format("gui.bind.info.hint"))));
    }

    private void renderItemSlots(Inventory inventory, PlayerData data) {
        int count = bindings.slotCount(data);
        for (int index = 0; index < count; index++) {
            int slot = SLOT_ROW * 9 + index;
            if (slot >= screen().size()) {
                break;
            }
            String skillId = bindings.slotSkill(data, index);
            SkillDefinition skill = skills.tree().get(skillId);
            inventory.setItem(slot, Icons.build(
                    Icons.material(screen().icon(skill == null ? ROLE_EMPTY : ROLE_SLOT)),
                    messages.format("gui.bind.slot.name",
                            "index", index + 1,
                            "skill", skill == null
                                    ? messages.format("gui.bind.empty")
                                    : skill.display()),
                    List.of(messages.format("gui.bind.slot.hint"))));
            slotToIndex.put(slot, index);
        }
    }

    private void renderCombos(Inventory inventory, PlayerData data) {
        int row = COMBO_ROW;
        for (HoldState hold : HoldState.values()) {
            int column = 0;
            for (InputTrigger trigger : bindings.availableTriggers()) {
                int slot = row * 9 + column;
                if (slot >= screen().size()) {
                    break;
                }
                String skillId = bindings.comboSkill(data, hold, trigger);
                SkillDefinition skill = skills.tree().get(skillId);
                inventory.setItem(slot, Icons.build(
                        Icons.material(screen().icon(skill == null ? ROLE_EMPTY : ROLE_COMBO)),
                        messages.format("gui.bind.combo.name",
                                "hold", messages.format("hold." + hold.name().toLowerCase(Locale.ROOT)),
                                "trigger", messages.format(
                                        "trigger." + trigger.name().toLowerCase(Locale.ROOT))),
                        List.of(messages.format("gui.bind.combo.skill",
                                        "skill", skill == null
                                                ? messages.format("gui.bind.empty")
                                                : skill.display()),
                                messages.format("gui.bind.combo.hint"))));
                slotToCombo.put(slot, new PlayerData.KeyCombo(hold, trigger, skillId));
                column++;
            }
            row++;
        }
    }

    @Override
    public void onClick(RpgPlayer rpgPlayer, int slot, InventoryClickEvent event) {
        Integer index = slotToIndex.get(slot);
        if (index != null) {
            handleItemSlot(rpgPlayer, index, event);
            render(rpgPlayer);
            return;
        }
        PlayerData.KeyCombo combo = slotToCombo.get(slot);
        if (combo != null) {
            handleCombo(rpgPlayer, combo, event);
            render(rpgPlayer);
        }
    }

    private void handleItemSlot(RpgPlayer rpgPlayer, int index, InventoryClickEvent event) {
        if (event.isRightClick()) {
            bindings.clearSlot(rpgPlayer, index);
            return;
        }
        String next = nextSkill(rpgPlayer.data(), bindings.slotSkill(rpgPlayer.data(), index));
        if (next == null) {
            messages.send(rpgPlayer.player(), "gui.bind.no-skills");
            return;
        }
        report(rpgPlayer, bindings.setSlot(rpgPlayer, index, next));
    }

    private void handleCombo(RpgPlayer rpgPlayer, PlayerData.KeyCombo combo,
                             InventoryClickEvent event) {
        if (event.isRightClick()) {
            bindings.clearCombo(rpgPlayer, combo.hold(), combo.trigger());
            return;
        }
        String next = nextSkill(rpgPlayer.data(), combo.skillId());
        if (next == null) {
            messages.send(rpgPlayer.player(), "gui.bind.no-skills");
            return;
        }
        // 같은 칸을 다시 거는 것이므로 먼저 비운다.
        bindings.clearCombo(rpgPlayer, combo.hold(), combo.trigger());
        report(rpgPlayer, bindings.setCombo(rpgPlayer, combo.hold(), combo.trigger(), next));
    }

    /**
     * 해금한 스킬을 차례로 돌린다.
     *
     * @param current 지금 걸려 있는 스킬. null 이면 첫 번째
     * @return 다음 스킬 id. 해금한 스킬이 없으면 null
     */
    private String nextSkill(PlayerData data, String current) {
        List<String> unlocked = new ArrayList<>(data.skill().unlocked());
        if (unlocked.isEmpty()) {
            return null;
        }
        if (current == null) {
            return unlocked.get(0);
        }
        int index = unlocked.indexOf(current);
        return unlocked.get((index + 1) % unlocked.size());
    }

    @Override
    public void onClose(RpgPlayer rpgPlayer) {
        guis.forget(rpgPlayer);
    }

    private void report(RpgPlayer rpgPlayer, BindingService.Result result) {
        if (result != BindingService.Result.OK) {
            messages.send(rpgPlayer.player(),
                    "gui.bind.result." + result.name().toLowerCase(Locale.ROOT).replace('_', '-'));
        }
    }
}
