package com.example.rpgcore.ui.gui;

import com.example.rpgcore.config.schema.GuiIcon;
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
 * 지시서 13장 2) 스킬트리 창.
 *
 * <p>직업 단계별로 스킬을 늘어놓고 해금 가능 / 불가 / 잠김을 구분한다.
 * 분기 선택은 되돌릴 수 없으므로 한 번 더 눌러야 확정된다.
 *
 * <p>스킬이 직업당 6~8개라서 자리를 설정에 하나씩 적지 않고 순서대로
 * 채운다. gui.yml 의 icons 는 상태별 아이템 종류를 정하는 데 쓴다.
 *
 * <p>좌클릭: 해금 또는 레벨 +1 / 시프트 좌클릭: 레벨 +10
 */
public final class SkillTreeGui extends Gui {

    private static final String ROLE_INFO = "info";
    private static final String ROLE_UNLOCKED = "unlocked";
    private static final String ROLE_AVAILABLE = "available";
    private static final String ROLE_LOCKED = "locked";
    private static final int SHIFT_AMOUNT = 10;

    private final SkillService skills;
    private final Messages messages;
    private final GuiManager guis;

    private final Map<Integer, String> slotToSkill = new HashMap<>();

    /** 분기 확정을 기다리는 스킬. 같은 칸을 한 번 더 누르면 확정된다. */
    private String pendingBranch;

    public SkillTreeGui(GuiScreen screen, SkillService skills, Messages messages,
                        GuiManager guis) {
        super(screen);
        this.skills = skills;
        this.messages = messages;
        this.guis = guis;
    }

    @Override
    public void render(RpgPlayer rpgPlayer) {
        Inventory inventory = getInventory();
        inventory.clear();
        slotToSkill.clear();

        PlayerData data = rpgPlayer.data();
        renderInfo(inventory, data);

        int slot = 0;
        for (SkillDefinition skill : skills.tree().ofJob(data.job().base())) {
            if (slot >= screen().size()) {
                break;
            }
            if (isInfoSlot(slot)) {
                slot++;
                continue;
            }
            boolean unlocked = data.skill().unlocked().contains(skill.id());
            SkillService.Result state = unlocked
                    ? SkillService.Result.ALREADY_UNLOCKED
                    : skills.canUnlock(data, skill);
            inventory.setItem(slot, Icons.build(Icons.material(screen().icon(roleOf(state))),
                    messages.format("gui.skill.entry.name",
                            "skill", skill.display(),
                            "level", data.skill().levelOf(skill.id()),
                            "max", skill.maxLevel()),
                    skillLore(data, skill, state)));
            slotToSkill.put(slot, skill.id());
            slot++;
        }
    }

    private boolean isInfoSlot(int slot) {
        GuiIcon icon = screen().icon(ROLE_INFO);
        return icon != null && icon.slot() == slot;
    }

    private void renderInfo(Inventory inventory, PlayerData data) {
        GuiIcon icon = screen().icon(ROLE_INFO);
        if (icon == null || icon.slot() < 0 || icon.slot() >= screen().size()) {
            return;
        }
        inventory.setItem(icon.slot(), Icons.build(Icons.material(icon),
                messages.format("gui.skill.info.name", "points", data.skill().points()),
                List.of(messages.format("gui.skill.info.hint", "shift", SHIFT_AMOUNT),
                        messages.format("gui.skill.info.branch"))));
    }

    private List<String> skillLore(PlayerData data, SkillDefinition skill,
                                   SkillService.Result state) {
        int level = data.skill().levelOf(skill.id());
        int shown = Math.max(1, level);
        List<String> lore = new ArrayList<>();
        lore.add(messages.format("gui.skill.entry.power",
                "value", round(skills.powerOf(data, skill, shown))));
        lore.add(messages.format("gui.skill.entry.mana",
                "value", round(skill.manaAt(shown))));
        lore.add(messages.format("gui.skill.entry.cooldown",
                "value", skill.cooldownAt(shown)));
        if (skill.inBranchGroup()) {
            lore.add(messages.format("gui.skill.entry.branch", "group", skill.branchGroup()));
        }
        lore.add(messages.format("gui.skill.state." + key(state)));
        if (skill.id().equals(pendingBranch)) {
            lore.add(messages.format("gui.skill.entry.confirm"));
        }
        return lore;
    }

    @Override
    public void onClick(RpgPlayer rpgPlayer, int slot, InventoryClickEvent event) {
        String skillId = slotToSkill.get(slot);
        if (skillId == null) {
            return;
        }
        SkillDefinition skill = skills.tree().get(skillId);
        if (skill == null) {
            return;
        }
        PlayerData data = rpgPlayer.data();

        if (data.skill().unlocked().contains(skillId)) {
            int amount = event.isShiftClick() ? SHIFT_AMOUNT : 1;
            report(rpgPlayer, skills.invest(rpgPlayer, skillId, amount));
            render(rpgPlayer);
            return;
        }

        SkillService.Result canUnlock = skills.canUnlock(data, skill);
        if (canUnlock != SkillService.Result.OK) {
            report(rpgPlayer, canUnlock);
            render(rpgPlayer);
            return;
        }

        // 지시서 13장: 분기 선택은 되돌릴 수 없다는 경고를 띄우고 확인을 받는다.
        if (skill.inBranchGroup() && !skillId.equals(pendingBranch)) {
            pendingBranch = skillId;
            messages.send(rpgPlayer.player(), "gui.skill.confirm-branch",
                    "skill", skill.display());
            render(rpgPlayer);
            return;
        }

        pendingBranch = null;
        report(rpgPlayer, skills.unlock(rpgPlayer, skillId));
        render(rpgPlayer);
    }

    @Override
    public void onClose(RpgPlayer rpgPlayer) {
        pendingBranch = null;
        guis.forget(rpgPlayer);
    }

    private void report(RpgPlayer rpgPlayer, SkillService.Result result) {
        messages.send(rpgPlayer.player(), "gui.skill.state." + key(result));
    }

    private static String roleOf(SkillService.Result state) {
        return switch (state) {
            case ALREADY_UNLOCKED -> ROLE_UNLOCKED;
            case OK -> ROLE_AVAILABLE;
            default -> ROLE_LOCKED;
        };
    }

    /** 열거형 이름을 messages.yml 경로로 쓸 수 있게 바꾼다. */
    private static String key(SkillService.Result result) {
        return result.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static long round(double value) {
        return Math.round(value);
    }
}
