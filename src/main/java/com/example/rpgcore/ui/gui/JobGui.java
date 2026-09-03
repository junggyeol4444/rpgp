package com.example.rpgcore.ui.gui;

import com.example.rpgcore.config.schema.GuiIcon;
import com.example.rpgcore.config.schema.GuiScreen;
import com.example.rpgcore.config.schema.JobSettings;
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
 * <p>3단계 범위는 3레벨 기본 직업 선택까지다.
 * 20·50레벨 전직 화면은 8·9단계에서 이 클래스에 붙인다.
 *
 * <p>고르면 되돌릴 수 없으므로 현재 상태를 함께 보여준다.
 */
public final class JobGui extends Gui {

    private static final String ROLE_INFO = "info";

    private final JobService jobs;
    private final Messages messages;
    private final GuiManager guis;

    /** 슬롯 -> 직업 id. */
    private final Map<Integer, String> slotToJob = new HashMap<>();

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
        slotToJob.clear();

        PlayerData data = rpgPlayer.data();
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
            boolean current = job.id().equals(data.job().base());
            inventory.setItem(slot, Icons.build(Icons.material(icon),
                    messages.format(current ? "gui.job.entry.current" : "gui.job.entry.name",
                            "job", job.display()),
                    jobLore(job, state, current)));
            slotToJob.put(slot, job.id());
        }
    }

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
        lore.add(messages.format("gui.job.state." + key(state)));
        inventory.setItem(icon.slot(), Icons.build(Icons.material(icon),
                messages.format("gui.job.info.name"), lore));
    }

    private List<String> jobLore(JobDefinition job, JobService.Result state, boolean current) {
        List<String> lore = new ArrayList<>();
        lore.add(messages.format("gui.job.entry.role", "role", job.role()));
        for (Map.Entry<String, Integer> bonus : job.statBonusPerLevel().entrySet()) {
            lore.add(messages.format("gui.job.entry.bonus",
                    "stat", bonus.getKey(), "value", bonus.getValue()));
        }
        if (current) {
            lore.add(messages.format("gui.job.entry.selected"));
        } else if (state == JobService.Result.OK) {
            lore.add(messages.format("gui.job.entry.click"));
        } else {
            lore.add(messages.format("gui.job.state." + key(state)));
        }
        return lore;
    }

    @Override
    public void onClick(RpgPlayer rpgPlayer, int slot, InventoryClickEvent event) {
        String jobId = slotToJob.get(slot);
        if (jobId == null) {
            return;
        }
        JobService.Result result = jobs.select(rpgPlayer, jobId);
        if (result == JobService.Result.OK) {
            messages.send(rpgPlayer.player(), "gui.job.selected",
                    "job", jobs.displayName(rpgPlayer.data()));
        } else {
            messages.send(rpgPlayer.player(), "gui.job.state." + key(result));
        }
        render(rpgPlayer);
    }

    @Override
    public void onClose(RpgPlayer rpgPlayer) {
        guis.forget(rpgPlayer);
    }

    /** 열거형 이름을 messages.yml 경로로 쓸 수 있게 바꾼다. */
    private static String key(JobService.Result result) {
        return result.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
