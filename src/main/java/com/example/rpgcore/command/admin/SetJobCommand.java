package com.example.rpgcore.command.admin;

import com.example.rpgcore.job.JobBranch;
import com.example.rpgcore.job.JobDefinition;
import com.example.rpgcore.job.JobService;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.util.CommandUtil;
import com.example.rpgcore.util.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /rpg admin setjob &lt;player&gt; base &lt;jobId&gt; — 직업 강제 지정.
 *
 * <p>지시서 12장은 base/tier1/tier2 를 받는다.
 * 2차 전직은 9단계라 지금은 base 와 tier1 까지 처리한다.
 */
public final class SetJobCommand implements AdminSubCommand {

    private static final String TIER_BASE = "base";
    private static final String TIER_1 = "tier1";

    private final PlayerManager players;
    private final JobService jobs;
    private final Messages messages;

    public SetJobCommand(PlayerManager players, JobService jobs, Messages messages) {
        this.players = players;
        this.jobs = jobs;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "setjob";
    }

    @Override
    public String descriptionKey() {
        return "command.admin.setjob.desc";
    }

    @Override
    public String argHint() {
        return "<player> <base|tier1> <id>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messages.send(sender, "common.usage", "usage", usage());
            return;
        }
        Player target = CommandUtil.onlinePlayer(args[0]);
        RpgPlayer rpgPlayer = target == null ? null : players.get(target);
        if (rpgPlayer == null) {
            messages.send(sender, "common.unknown-player", "name", args[0]);
            return;
        }
        JobService.Result result;
        if (TIER_BASE.equalsIgnoreCase(args[1])) {
            result = jobs.forceSetBase(rpgPlayer, args[2]);
        } else if (TIER_1.equalsIgnoreCase(args[1])) {
            result = jobs.forceSetTier1(rpgPlayer, args[2]);
        } else {
            messages.send(sender, "admin.setjob.tier-not-supported", "tier", args[1]);
            return;
        }
        if (result != JobService.Result.OK) {
            messages.send(sender, "admin.setjob.failed",
                    "id", args[2], "reason", messages.format("gui.job.state."
                            + result.name().toLowerCase(Locale.ROOT).replace('_', '-')));
            return;
        }
        messages.send(sender, "admin.setjob.done",
                "player", target.getName(), "job", jobs.displayName(rpgPlayer.data()));
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            return players.onlinePlayers().stream().map(p -> p.player().getName()).toList();
        }
        if (args.length == 2) {
            return List.of(TIER_BASE, TIER_1);
        }
        if (args.length == 3) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            List<String> ids = new ArrayList<>();
            if (TIER_BASE.equalsIgnoreCase(args[1])) {
                for (JobDefinition job : jobs.tree().baseJobs()) {
                    if (job.id().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        ids.add(job.id());
                    }
                }
            } else if (TIER_1.equalsIgnoreCase(args[1])) {
                // 대상 플레이어의 기본 직업에 달린 분기만 보여준다.
                Player target = CommandUtil.onlinePlayer(args[0]);
                RpgPlayer rpgPlayer = target == null ? null : players.get(target);
                if (rpgPlayer != null) {
                    for (JobBranch branch : jobs.tier1Choices(rpgPlayer.data())) {
                        if (branch.id().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                            ids.add(branch.id());
                        }
                    }
                }
            }
            return ids;
        }
        return List.of();
    }
}
