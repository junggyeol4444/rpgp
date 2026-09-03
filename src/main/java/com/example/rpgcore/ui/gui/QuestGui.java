package com.example.rpgcore.ui.gui;

import com.example.rpgcore.config.schema.GuiIcon;
import com.example.rpgcore.config.schema.GuiScreen;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.player.data.PlayerData;
import com.example.rpgcore.quest.QuestDefinition;
import com.example.rpgcore.quest.QuestProgress;
import com.example.rpgcore.quest.QuestService;
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
 * 지시서 13장 4) 퀘스트 목록 · 수락 창.
 *
 * <p>수락 가능 / 진행 중 / 완료 탭을 두고 목표 진행도를 보여준다.
 *
 * <p>수락 가능 탭에서 좌클릭하면 수주하고, 진행 중 탭에서 우클릭하면
 * 포기한다. 목표를 다 채운 퀘스트는 좌클릭으로 완료 처리한다.
 */
public final class QuestGui extends Gui {

    /** 탭. */
    private enum Tab {
        AVAILABLE, ACTIVE, COMPLETED
    }

    private static final String ROLE_AVAILABLE = "available";
    private static final String ROLE_ACTIVE = "active";
    private static final String ROLE_COMPLETED = "completed";
    private static final String ROLE_ENTRY = "entry";
    /** 목록이 시작되는 칸. 첫 줄은 탭 자리다. */
    private static final int LIST_START = 9;

    private final QuestService quests;
    private final Messages messages;
    private final GuiManager guis;

    private final Map<Integer, String> slotToQuest = new HashMap<>();
    private final Map<Integer, Tab> slotToTab = new HashMap<>();
    private Tab tab = Tab.AVAILABLE;

    public QuestGui(GuiScreen screen, QuestService quests, Messages messages, GuiManager guis) {
        super(screen);
        this.quests = quests;
        this.messages = messages;
        this.guis = guis;
    }

    @Override
    public void render(RpgPlayer rpgPlayer) {
        Inventory inventory = getInventory();
        inventory.clear();
        slotToQuest.clear();
        slotToTab.clear();

        renderTab(inventory, ROLE_AVAILABLE, Tab.AVAILABLE);
        renderTab(inventory, ROLE_ACTIVE, Tab.ACTIVE);
        renderTab(inventory, ROLE_COMPLETED, Tab.COMPLETED);
        renderList(inventory, rpgPlayer.data());
    }

    private void renderTab(Inventory inventory, String role, Tab target) {
        GuiIcon icon = screen().icon(role);
        if (icon == null || icon.slot() < 0 || icon.slot() >= screen().size()) {
            return;
        }
        inventory.setItem(icon.slot(), Icons.build(Icons.material(icon),
                messages.format("gui.quest.tab." + role),
                List.of(messages.format(target == tab
                        ? "gui.quest.tab.selected"
                        : "gui.quest.tab.hint"))));
        slotToTab.put(icon.slot(), target);
    }

    private void renderList(Inventory inventory, PlayerData data) {
        int slot = LIST_START;
        for (QuestDefinition quest : quests.definitions().values()) {
            if (slot >= screen().size()) {
                break;
            }
            if (!belongsToTab(data, quest)) {
                continue;
            }
            inventory.setItem(slot, Icons.build(Icons.material(screen().icon(ROLE_ENTRY)),
                    messages.format("gui.quest.entry.name", "quest", quest.display()),
                    questLore(data, quest)));
            slotToQuest.put(slot, quest.id());
            slot++;
        }
    }

    private boolean belongsToTab(PlayerData data, QuestDefinition quest) {
        boolean active = data.quest().active().containsKey(quest.id());
        boolean completed = data.quest().completed().contains(quest.id());
        return switch (tab) {
            case ACTIVE -> active;
            case COMPLETED -> completed;
            case AVAILABLE -> !active
                    && quests.canAccept(data, quest) == QuestService.Result.OK;
        };
    }

    private List<String> questLore(PlayerData data, QuestDefinition quest) {
        List<String> lore = new ArrayList<>();
        lore.add(messages.format("gui.quest.entry.type",
                "type", messages.format("quest.type." + quest.type().name().toLowerCase(Locale.ROOT))));
        lore.add(messages.format("gui.quest.entry.level", "level", quest.requireLevel()));

        QuestProgress progress = quests.progressOf(data, quest);
        for (int i = 0; i < quest.objectives().size(); i++) {
            Objective objective = quest.objectives().get(i);
            int count = progress == null ? 0 : progress.count(i);
            lore.add(messages.format("gui.quest.entry.objective",
                    "type", messages.format(
                            "objective." + objective.type().name().toLowerCase(Locale.ROOT)),
                    "key", objective.key(),
                    "count", Math.min(count, objective.amount()),
                    "amount", objective.amount()));
        }
        if (!quest.reward().isEmpty()) {
            lore.add(messages.format("gui.quest.entry.reward",
                    "exp", Math.round(quest.reward().combatExp()),
                    "points", quest.reward().skillPoints()));
        }
        lore.add(messages.format("gui.quest.entry.hint." + tab.name().toLowerCase(Locale.ROOT)));
        return lore;
    }

    @Override
    public void onClick(RpgPlayer rpgPlayer, int slot, InventoryClickEvent event) {
        Tab target = slotToTab.get(slot);
        if (target != null) {
            tab = target;
            render(rpgPlayer);
            return;
        }
        String questId = slotToQuest.get(slot);
        if (questId == null) {
            return;
        }

        QuestService.Result result;
        if (tab == Tab.AVAILABLE) {
            result = quests.accept(rpgPlayer, questId);
        } else if (tab == Tab.ACTIVE) {
            result = event.isRightClick()
                    ? quests.abandon(rpgPlayer, questId)
                    : quests.complete(rpgPlayer, questId);
        } else {
            return;
        }
        if (result != QuestService.Result.OK) {
            messages.send(rpgPlayer.player(), "quest.result." + key(result));
        }
        render(rpgPlayer);
    }

    @Override
    public void onClose(RpgPlayer rpgPlayer) {
        guis.forget(rpgPlayer);
    }

    /** 열거형 이름을 messages.yml 경로로 쓸 수 있게 바꾼다. */
    private static String key(QuestService.Result result) {
        return result.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
