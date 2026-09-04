package com.example.rpgcore.ui.gui;

import com.example.rpgcore.config.schema.GuiIcon;
import com.example.rpgcore.config.schema.GuiScreen;
import com.example.rpgcore.config.schema.JobSettings;
import com.example.rpgcore.job.JobBranch;
import com.example.rpgcore.job.JobDefinition;
import com.example.rpgcore.job.JobService;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.player.data.PlayerData;
import com.example.rpgcore.util.Messages;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * 지시서 13장 3) 직업 선택·전직 창.
 *
 * <p>기본 직업을 아직 안 골랐으면 7개 중 고르는 화면이,
 * 골랐으면 1차 전직 분기를 고르는 화면이 나온다.
 * (기획서 5장: 3레벨 기본 직업, 20레벨 1차 전직)
 *
 * <p>2차 전직은 9단계에서 이 클래스에 붙는다.
 *
 * <p>고르면 되돌릴 수 없으므로 한 번 더 눌러야 확정된다.
 */
public final class JobGui extends Gui {

    private static final String ROLE_INFO = "info";
    private static final String ROLE_BRANCH = "branch";
    /** 분기 목록이 시작되는 칸. */
    private static final int BRANCH_START = 10;

    private final JobService jobs;
    private final Messages messages;
    private final GuiManager guis;

    /** 슬롯 -> 고를 대상 id. 기본 직업 화면과 전직 화면이 같이 쓴다. */
    private final Map<Integer, String> slotToChoice = new HashMap<>();

    /** 전직 확정을 기다리는 분기. 같은 칸을 한 번 더 누르면 확정된다. */
    private String pendingBranch;

    public JobGui(GuiScreen screen, JobService jobs, Messages messages, GuiManager guis) {
        super(screen);
        this.jobs = jobs;
        this.messages = messages;
        this.guis = guis;
    }

    @Override
    public void render(RpgPlayer rpgPlayer) {
        Inventory inventory = getInventory();
        inventory.clear();
        slotToChoice.clear();

        PlayerData data = rpgPlayer.data();
        if (data.job().hasBase()) {
            renderTier1(inventory, data);
        } else {
            renderBaseJobs(inventory, data);
        }
    }

    // ------------------------------------------------------------
    // 기본 직업 선택 (3단계)
    // ------------------------------------------------------------

    private void renderBaseJobs(Inventory inventory, PlayerData data) {
        JobSettings settings = jobs.settings();
        JobService.Result state = jobs.canSelect(data);
        renderInfo(inventory, data, settings, state);

        int fallbackSlot = 0;
        for (JobDefinition job : jobs.tree().baseJobs()) {
            GuiIcon icon = screen().icon(job.id());
            int slot = icon != null ? icon.slot() : fallbackSlot++;
            if (slot < 0 || slot >= screen().size()) {
                continue;
            }
            inventory.setItem(slot, Icons.build(Icons.material(icon),
                    messages.format("gui.job.entry.name", "job", job.display()),
                    baseLore(job, state)));
            slotToChoice.put(slot, job.id());
        }
    }

    private List<String> baseLore(JobDefinition job, JobService.Result state) {
        List<String> lore = new ArrayList<>();
        lore.add(messages.format("gui.job.entry.role", "role", job.role()));
        for (Map.Entry<String, Integer> bonus : job.statBonusPerLevel().entrySet()) {
            lore.add(messages.format("gui.job.entry.bonus",
                    "stat", bonus.getKey(), "value", bonus.getValue()));
        }
        lore.add(state == JobService.Result.OK
                ? messages.format("gui.job.entry.click")
                : messages.format("gui.job.state." + key(state)));
        return lore;
    }

    // ------------------------------------------------------------
    // 1차 전직 (8단계)
    // ------------------------------------------------------------

    private void renderTier1(Inventory inventory, PlayerData data) {
        JobSettings settings = jobs.settings();
        JobService.Result state = jobs.canAdvanceTier1(data);
        renderInfo(inventory, data, settings, state);

        String current = data.job().tier1();
        int slot = BRANCH_START;
        for (JobBranch branch : jobs.tier1Choices(data)) {
            if (slot >= screen().size()) {
                break;
            }
            boolean chosen = branch.id().equals(current);
            inventory.setItem(slot, Icons.build(Icons.material(screen().icon(ROLE_BRANCH)),
                    messages.format(chosen ? "gui.job.entry.current" : "gui.job.entry.name",
                            "job", branch.display()),
                    branchLore(branch, state, chosen)));
            slotToChoice.put(slot, branch.id());
            slot++;
        }
    }

    private List<String> branchLore(JobBranch branch, JobService.Result state, boolean chosen) {
        List<String> lore = new ArrayList<>();
        for (Map.Entry<String, Integer> bonus : branch.statBonusPerLevel().entrySet()) {
            lore.add(messages.format("gui.job.entry.bonus",
                    "stat", bonus.getKey(), "value", bonus.getValue()));
        }
        if (chosen) {
            lore.add(messages.format("gui.job.entry.selected"));
        } else if (state == JobService.Result.OK) {
            lore.add(messages.format("gui.job.entry.click"));
        } else {
            lore.add(messages.format("gui.job.state." + key(state)));
        }
        if (branch.id().equals(pendingBranch)) {
            lore.add(messages.format("gui.job.entry.confirm"));
        }
        return lore;
    }

    // ------------------------------------------------------------
    // 공통
    // ------------------------------------------------------------

    private void renderInfo(Inventory inventory, PlayerData data,
                            JobSettings settings, JobService.Result state) {
        GuiIcon icon = screen().icon(ROLE_INFO);
        if (icon == null || icon.slot() < 0 || icon.slot() >= screen().size()) {
            return;
        }
        String current = jobs.displayName(data);
        List<String> lore = new ArrayList<>();
        lore.add(current == null
                ? messages.format("gui.job.info.none", "level", settings.jobSelectLevel())
                : messages.format("gui.job.info.current", "job", current));
        if (data.job().hasBase()) {
            lore.add(messages.format("gui.job.info.tier1-level",
                    "level", settings.tier1Level(), "quest", settings.tier1Quest()));
        }
        lore.add(messages.format("gui.job.state." + key(state)));
        inventory.setItem(icon.slot(), Icons.build(Icons.material(icon),
                messages.format("gui.job.info.name"), lore));
    }

    @Override
    public void onClick(RpgPlayer rpgPlayer, int slot, InventoryClickEvent event) {
        String choice = slotToChoice.get(slot);
        if (choice == null) {
            return;
        }
        PlayerData data = rpgPlayer.data();

        if (!data.job().hasBase()) {
            report(rpgPlayer, jobs.select(rpgPlayer, choice), "gui.job.selected");
            render(rpgPlayer);
            return;
        }

        // 전직은 되돌릴 수 없으므로 확인을 받는다. (지시서 13장)
        if (jobs.canAdvanceTier1(data) == JobService.Result.OK
                && !choice.equals(pendingBranch)) {
            pendingBranch = choice;
            messages.send(rpgPlayer.player(), "gui.job.confirm-advance");
            render(rpgPlayer);
            return;
        }
        pendingBranch = null;
        report(rpgPlayer, jobs.advanceTier1(rpgPlayer, choice), "gui.job.advanced");
        render(rpgPlayer);
    }

    private void report(RpgPlayer rpgPlayer, JobService.Result result, String okKey) {
        if (result == JobService.Result.OK) {
            messages.send(rpgPlayer.player(), okKey,
                    "job", jobs.displayName(rpgPlayer.data()));
        } else {
            messages.send(rpgPlayer.player(), "gui.job.state." + key(result));
        }
    }

    @Override
    public void onClose(RpgPlayer rpgPlayer) {
        pendingBranch = null;
        guis.forget(rpgPlayer);
    }

    /** 열거형 이름을 messages.yml 경로로 쓸 수 있게 바꾼다. */
    private static String key(JobService.Result result) {
        return result.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
