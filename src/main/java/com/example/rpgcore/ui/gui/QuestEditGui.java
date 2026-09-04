package com.example.rpgcore.ui.gui;

import com.example.rpgcore.config.schema.GuiIcon;
import com.example.rpgcore.config.schema.GuiScreen;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.quest.editor.QuestDraft;
import com.example.rpgcore.quest.editor.QuestEditorService;
import com.example.rpgcore.quest.objective.Objective;
import com.example.rpgcore.util.Messages;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * 지시서 14장 10단계 — 퀘스트 하나를 편집한다.
 *
 * <p>숫자와 종류는 클릭으로, 글자가 필요한 값(표시 이름 · 직업 · 목표
 * 대상)은 채팅으로 받는다. 인벤토리 화면만으로는 글자를 받을 수 없다.
 *
 * <p>목표 칸: 좌클릭 종류 순환 / 시프트 좌클릭 대상·개수 / 우클릭 삭제
 */
public final class QuestEditGui extends Gui {

    private static final String ROLE_DISPLAY = "display";
    private static final String ROLE_TYPE = "type";
    private static final String ROLE_LEVEL = "level";
    private static final String ROLE_JOB = "job";
    private static final String ROLE_REPEAT = "repeat";
    private static final String ROLE_EXP = "exp";
    private static final String ROLE_SKILLPOINT = "skillpoint";
    private static final String ROLE_STATPOINT = "statpoint";
    private static final String ROLE_SAVE = "save";
    private static final String ROLE_ADD = "addobjective";
    private static final String ROLE_OBJECTIVE = "objective";

    /** 목표 목록이 시작되는 칸. */
    private static final int OBJECTIVE_START = 18;
    private static final int SHIFT_STEP = 10;

    private final QuestEditorService editor;
    private final Messages messages;
    private final GuiManager guis;

    private final Map<Integer, String> slotToRole = new HashMap<>();
    private final Map<Integer, Integer> slotToObjective = new HashMap<>();

    public QuestEditGui(GuiScreen screen, QuestEditorService editor, Messages messages,
                        GuiManager guis) {
        super(screen);
        this.editor = editor;
        this.messages = messages;
        this.guis = guis;
    }

    @Override
    public void render(RpgPlayer rpgPlayer) {
        Inventory inventory = getInventory();
        inventory.clear();
        slotToRole.clear();
        slotToObjective.clear();

        QuestDraft draft = editor.draft(rpgPlayer);
        if (draft == null) {
            return;
        }

        put(inventory, ROLE_DISPLAY, "gui.editor.field.display",
                List.of(messages.format("gui.editor.value", "value", draft.display()),
                        messages.format("gui.editor.hint.chat")),
                "id", draft.id());
        put(inventory, ROLE_TYPE, "gui.editor.field.type",
                List.of(messages.format("gui.editor.value", "value",
                                messages.format("quest.type."
                                        + draft.type().name().toLowerCase(Locale.ROOT))),
                        messages.format("gui.editor.hint.cycle")));
        put(inventory, ROLE_LEVEL, "gui.editor.field.level",
                List.of(messages.format("gui.editor.value", "value", draft.requireLevel()),
                        messages.format("gui.editor.hint.number", "shift", SHIFT_STEP)));
        put(inventory, ROLE_JOB, "gui.editor.field.job",
                List.of(messages.format("gui.editor.value", "value",
                                draft.requireJob() == null
                                        ? messages.format("gui.editor.none")
                                        : draft.requireJob()),
                        messages.format("gui.editor.hint.chat")));
        put(inventory, ROLE_REPEAT, "gui.editor.field.repeat",
                List.of(messages.format("gui.editor.value", "value",
                                messages.format(draft.repeatable()
                                        ? "state.enabled" : "state.disabled")),
                        messages.format("gui.editor.hint.toggle")));
        put(inventory, ROLE_EXP, "gui.editor.field.exp",
                List.of(messages.format("gui.editor.value",
                                "value", Math.round(draft.combatExp())),
                        messages.format("gui.editor.hint.number", "shift", SHIFT_STEP)));
        put(inventory, ROLE_SKILLPOINT, "gui.editor.field.skillpoint",
                List.of(messages.format("gui.editor.value", "value", draft.skillPoints()),
                        messages.format("gui.editor.hint.number", "shift", SHIFT_STEP)));
        put(inventory, ROLE_STATPOINT, "gui.editor.field.statpoint",
                List.of(messages.format("gui.editor.value", "value", draft.statPoints()),
                        messages.format("gui.editor.hint.number", "shift", SHIFT_STEP)));
        put(inventory, ROLE_ADD, "gui.editor.field.add",
                List.of(messages.format("gui.editor.hint.add")));
        put(inventory, ROLE_SAVE, "gui.editor.field.save",
                List.of(draft.isValid()
                        ? messages.format("gui.editor.hint.save")
                        : messages.format("gui.editor.hint.no-objective")));

        renderObjectives(inventory, draft);
    }

    private void put(Inventory inventory, String role, String nameKey,
                     List<String> lore, Object... nameArgs) {
        GuiIcon icon = screen().icon(role);
        if (icon == null || icon.slot() < 0 || icon.slot() >= screen().size()) {
            return;
        }
        inventory.setItem(icon.slot(),
                Icons.build(Icons.material(icon), messages.format(nameKey, nameArgs), lore));
        slotToRole.put(icon.slot(), role);
    }

    private void renderObjectives(Inventory inventory, QuestDraft draft) {
        int slot = OBJECTIVE_START;
        for (int i = 0; i < draft.objectives().size(); i++) {
            if (slot >= screen().size()) {
                break;
            }
            Objective objective = draft.objectives().get(i);
            List<String> lore = new ArrayList<>();
            lore.add(messages.format("gui.editor.objective.target",
                    "key", objective.key(), "amount", objective.amount()));
            lore.add(messages.format("gui.editor.objective.hint"));
            inventory.setItem(slot, Icons.build(Icons.material(screen().icon(ROLE_OBJECTIVE)),
                    messages.format("gui.editor.objective.name",
                            "index", i + 1,
                            "type", messages.format("objective."
                                    + objective.type().name().toLowerCase(Locale.ROOT))),
                    lore));
            slotToObjective.put(slot, i);
            slot++;
        }
    }

    @Override
    public void onClick(RpgPlayer rpgPlayer, int slot, InventoryClickEvent event) {
        QuestDraft draft = editor.draft(rpgPlayer);
        if (draft == null) {
            return;
        }

        Integer objectiveIndex = slotToObjective.get(slot);
        if (objectiveIndex != null) {
            handleObjective(rpgPlayer, draft, objectiveIndex, event);
            return;
        }

        String role = slotToRole.get(slot);
        if (role == null) {
            return;
        }
        int step = event.isShiftClick() ? SHIFT_STEP : 1;
        int sign = event.isRightClick() ? -1 : 1;

        switch (role) {
            case ROLE_DISPLAY -> editor.prompt(rpgPlayer, "editor.prompt.display", input -> {
                draft.display(input);
                guis.open(rpgPlayer, this);
            });
            case ROLE_TYPE -> draft.cycleType();
            case ROLE_LEVEL -> draft.requireLevel(draft.requireLevel() + step * sign);
            case ROLE_JOB -> editor.prompt(rpgPlayer, "editor.prompt.job", input -> {
                draft.requireJob(input.equalsIgnoreCase("none") ? null : input);
                guis.open(rpgPlayer, this);
            });
            case ROLE_REPEAT -> draft.toggleRepeatable();
            case ROLE_EXP -> draft.addCombatExp(step * sign * 10.0);
            case ROLE_SKILLPOINT -> draft.addSkillPoints(step * sign);
            case ROLE_STATPOINT -> draft.addStatPoints(step * sign);
            case ROLE_ADD -> draft.addObjective();
            case ROLE_SAVE -> editor.save(rpgPlayer, draft);
            default -> {
                return;
            }
        }
        render(rpgPlayer);
    }

    private void handleObjective(RpgPlayer rpgPlayer, QuestDraft draft, int index,
                                 InventoryClickEvent event) {
        if (event.isRightClick()) {
            draft.removeObjective(index);
            render(rpgPlayer);
            return;
        }
        if (event.isShiftClick()) {
            editor.prompt(rpgPlayer, "editor.prompt.objective", input -> {
                String[] parts = input.split("\\s+");
                int amount = 1;
                if (parts.length > 1) {
                    try {
                        amount = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        messages.send(rpgPlayer.player(), "common.invalid-number",
                                "input", parts[1]);
                        return;
                    }
                }
                draft.setObjectiveTarget(index, parts[0], amount);
                guis.open(rpgPlayer, this);
            });
            return;
        }
        draft.cycleObjectiveType(index);
        render(rpgPlayer);
    }

    @Override
    public void onClose(RpgPlayer rpgPlayer) {
        guis.forget(rpgPlayer);
    }
}
