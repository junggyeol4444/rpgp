package com.example.rpgcore.ui.gui;

import com.example.rpgcore.config.schema.GuiIcon;
import com.example.rpgcore.config.schema.GuiScreen;
import com.example.rpgcore.config.schema.ResetSettings;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.player.data.PlayerData;
import com.example.rpgcore.stat.DerivedStat;
import com.example.rpgcore.stat.StatService;
import com.example.rpgcore.stat.StatType;
import com.example.rpgcore.util.Messages;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * 지시서 13장 1) 스탯 분배 창.
 *
 * <p>능력치별 현재값과 파생 수치, 남은 포인트를 보여주고,
 * 분배 버튼과 비용이 붙은 리셋 버튼을 둔다.
 *
 * <p>좌클릭 +1, 시프트 좌클릭 +10.
 */
public final class StatGui extends Gui {

    private static final String ROLE_INFO = "info";
    private static final String ROLE_RESET = "reset";
    private static final int SHIFT_AMOUNT = 10;

    private final StatService stats;
    private final Messages messages;
    private final GuiManager guis;

    /** 슬롯 -> 능력치 id. 클릭 처리에 쓴다. */
    private final Map<Integer, String> slotToStat = new HashMap<>();
    private int resetSlot = -1;

    public StatGui(GuiScreen screen, StatService stats, Messages messages, GuiManager guis) {
        super(screen);
        this.stats = stats;
        this.messages = messages;
        this.guis = guis;
    }

    @Override
    public void render(RpgPlayer rpgPlayer) {
        Inventory inventory = getInventory();
        inventory.clear();
        slotToStat.clear();
        resetSlot = -1;

        PlayerData data = rpgPlayer.data();
        renderInfo(inventory, rpgPlayer);

        int fallbackSlot = 0;
        for (StatType stat : stats.settings().stats().values()) {
            GuiIcon icon = screen().icon(stat.id());
            int slot = icon != null ? icon.slot() : fallbackSlot++;
            if (slot < 0 || slot >= screen().size()) {
                continue;
            }
            inventory.setItem(slot, Icons.build(Icons.material(icon),
                    messages.format("gui.stat.entry.name",
                            "stat", stat.display(),
                            "points", data.combat().stat(stat.id())),
                    statLore(stat)));
            slotToStat.put(slot, stat.id());
        }

        renderReset(inventory, rpgPlayer);
    }

    private void renderInfo(Inventory inventory, RpgPlayer rpgPlayer) {
        GuiIcon icon = screen().icon(ROLE_INFO);
        if (icon == null || icon.slot() < 0 || icon.slot() >= screen().size()) {
            return;
        }
        List<String> lore = new ArrayList<>();
        for (DerivedStat derived : DerivedStat.values()) {
            lore.add(messages.format("gui.stat.derived",
                    "name", messages.format("derived." + derived.configKey()),
                    "value", format(rpgPlayer.derived().get(derived))));
        }
        inventory.setItem(icon.slot(), Icons.build(Icons.material(icon),
                messages.format("gui.stat.info.name",
                        "points", rpgPlayer.data().combat().statPoints()),
                lore));
    }

    private void renderReset(Inventory inventory, RpgPlayer rpgPlayer) {
        ResetSettings reset = stats.settings().reset();
        if (!reset.allowed()) {
            return;
        }
        GuiIcon icon = screen().icon(ROLE_RESET);
        if (icon == null || icon.slot() < 0 || icon.slot() >= screen().size()) {
            return;
        }
        long cost = stats.resetCost(rpgPlayer.data());
        inventory.setItem(icon.slot(), Icons.build(Icons.material(icon),
                messages.format("gui.stat.reset.name"),
                List.of(messages.format("gui.stat.reset.cost",
                                "amount", cost, "currency", reset.currencyId()),
                        messages.format("gui.stat.reset.owned",
                                "amount", rpgPlayer.data().currency(reset.currencyId())))));
        resetSlot = icon.slot();
    }

    private List<String> statLore(StatType stat) {
        List<String> lore = new ArrayList<>();
        for (Map.Entry<DerivedStat, Double> entry : stat.perPoint().entrySet()) {
            lore.add(messages.format("gui.stat.entry.per-point",
                    "name", messages.format("derived." + entry.getKey().configKey()),
                    "value", format(entry.getValue())));
        }
        lore.add(messages.format("gui.stat.entry.hint", "shift", SHIFT_AMOUNT));
        return lore;
    }

    @Override
    public void onClick(RpgPlayer rpgPlayer, int slot, InventoryClickEvent event) {
        if (slot == resetSlot) {
            StatService.Result result = stats.reset(rpgPlayer);
            messages.send(rpgPlayer.player(), "gui.stat.result." + key(result));
            render(rpgPlayer);
            return;
        }
        String statId = slotToStat.get(slot);
        if (statId == null) {
            return;
        }
        int amount = event.isShiftClick() ? SHIFT_AMOUNT : 1;
        StatService.Result result = stats.allocate(rpgPlayer, statId, amount);
        if (result != StatService.Result.OK) {
            messages.send(rpgPlayer.player(), "gui.stat.result." + key(result));
        }
        render(rpgPlayer);
    }

    @Override
    public void onClose(RpgPlayer rpgPlayer) {
        guis.forget(rpgPlayer);
    }

    /** 열거형 이름을 messages.yml 경로로 쓸 수 있게 바꾼다. */
    private static String key(StatService.Result result) {
        return result.name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
    }

    private static String format(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(Math.round(value * 100.0) / 100.0);
    }
}
