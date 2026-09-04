package com.example.rpgcore.ui.gui;

import com.example.rpgcore.config.schema.GuiIcon;
import com.example.rpgcore.config.schema.GuiScreen;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.quest.QuestDefinition;
import com.example.rpgcore.quest.QuestService;
import com.example.rpgcore.quest.editor.QuestDraft;
import com.example.rpgcore.quest.editor.QuestEditorService;
import com.example.rpgcore.util.Messages;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * 지시서 14장 10단계 — 퀘스트 목록과 새로 만들기.
 *
 * <p>좌클릭으로 편집 화면에 들어가고, 새로 만들기 칸을 누르면 채팅으로
 * 퀘스트 id 를 받는다.
 */
public final class QuestEditorGui extends Gui {

    private static final String ROLE_NEW = "new";
    private static final String ROLE_ENTRY = "entry";
    /** 목록이 시작되는 칸. 첫 줄은 새로 만들기 자리다. */
    private static final int LIST_START = 9;
    /** 퀘스트 id 로 쓸 수 있는 글자. */
    private static final String ID_PATTERN = "[a-z0-9_]{1,40}";

    private final GuiScreen editScreen;
    private final QuestService quests;
    private final QuestEditorService editor;
    private final Messages messages;
    private final GuiManager guis;

    private final Map<Integer, String> slotToQuest = new HashMap<>();
    private int newSlot = -1;

    /**
     * @param screen     목록 화면 정의 (gui.yml 의 questeditor)
     * @param editScreen 편집 화면 정의 (gui.yml 의 questedit). 칸 수와 아이콘
     *                   역할이 목록 화면과 달라서 따로 받는다
     */
    public QuestEditorGui(GuiScreen screen, GuiScreen editScreen, QuestService quests,
                          QuestEditorService editor, Messages messages, GuiManager guis) {
        super(screen);
        this.editScreen = editScreen;
        this.quests = quests;
        this.editor = editor;
        this.messages = messages;
        this.guis = guis;
    }

    @Override
    public void render(RpgPlayer rpgPlayer) {
        Inventory inventory = getInventory();
        inventory.clear();
        slotToQuest.clear();
        newSlot = -1;

        GuiIcon newIcon = screen().icon(ROLE_NEW);
        if (newIcon != null && newIcon.slot() >= 0 && newIcon.slot() < screen().size()) {
            inventory.setItem(newIcon.slot(), Icons.build(Icons.material(newIcon),
                    messages.format("gui.editor.new.name"),
                    List.of(messages.format("gui.editor.new.hint"))));
            newSlot = newIcon.slot();
        }

        Set<String> managed = editor.writer().managedIds();
        int slot = LIST_START;
        for (QuestDefinition quest : quests.definitions().values()) {
            if (slot >= screen().size()) {
                break;
            }
            List<String> lore = new ArrayList<>();
            lore.add(messages.format("gui.editor.entry.id", "id", quest.id()));
            lore.add(messages.format("gui.editor.entry.type",
                    "type", messages.format("quest.type."
                            + quest.type().name().toLowerCase(Locale.ROOT))));
            lore.add(messages.format("gui.editor.entry.objectives",
                    "count", quest.objectives().size()));
            if (managed.contains(quest.id())) {
                lore.add(messages.format("gui.editor.entry.hint"));
            } else {
                // 손으로 적은 퀘스트도 열어서 고칠 수는 있다. 다만 저장하면
                // quests/editor.yml 에 복사본이 생기고, 뒤에 읽히는 그쪽이 이긴다.
                // 지우기는 그 복사본에만 걸리므로 여기서는 막는다.
                lore.add(messages.format("gui.editor.entry.copied"));
                lore.add(messages.format("gui.editor.entry.copied2"));
                lore.add(messages.format("gui.editor.entry.hint-edit-only"));
            }
            inventory.setItem(slot, Icons.build(Icons.material(screen().icon(ROLE_ENTRY)),
                    messages.format("gui.editor.entry.name", "quest", quest.display()),
                    lore));
            slotToQuest.put(slot, quest.id());
            slot++;
        }
    }

    @Override
    public void onClick(RpgPlayer rpgPlayer, int slot, InventoryClickEvent event) {
        if (slot == newSlot) {
            askForNewId(rpgPlayer);
            return;
        }
        String questId = slotToQuest.get(slot);
        if (questId == null) {
            return;
        }
        QuestDefinition quest = quests.definition(questId);
        if (quest == null) {
            return;
        }
        if (event.isRightClick() && editor.writer().managedIds().contains(questId)) {
            // 지우기는 IO 스레드를 거치므로, 다 끝난 뒤에 목록을 다시 그린다.
            editor.delete(rpgPlayer, questId, () -> render(rpgPlayer));
            return;
        }
        editor.draft(rpgPlayer, QuestDraft.from(quest));
        guis.open(rpgPlayer, new QuestEditGui(editScreen, editor, messages, guis));
    }

    private void askForNewId(RpgPlayer rpgPlayer) {
        editor.prompt(rpgPlayer, "editor.prompt.id", input -> {
            String id = input.toLowerCase(Locale.ROOT);
            if (!id.matches(ID_PATTERN)) {
                messages.send(rpgPlayer.player(), "editor.prompt.bad-id", "input", input);
                return;
            }
            if (quests.definition(id) != null) {
                messages.send(rpgPlayer.player(), "editor.prompt.duplicate-id", "id", id);
                return;
            }
            editor.draft(rpgPlayer, new QuestDraft(id));
            guis.open(rpgPlayer, new QuestEditGui(editScreen, editor, messages, guis));
        });
    }

    @Override
    public void onClose(RpgPlayer rpgPlayer) {
        guis.forget(rpgPlayer);
    }
}
