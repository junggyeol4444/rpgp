package com.example.rpgcore.command.admin;

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
 * <p>지시서 12장은 base/tier1/tier2 를 받도록 되어 있지만, 전직 자체가
 * 8·9단계라 지금은 base 만 처리한다. 나머지는 해당 단계에서 넓힌다.
 * (지시서 15장: 다음 단계 기능을 미리 절반만 만들어두지 않는다)
 */
public final class SetJobCommand implements AdminSubCommand {

    private static final String TIER_BASE = "base";

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
        return "<player> base <jobId>";
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
        if (!TIER_BASE.equalsIgnoreCase(args[1])) {
            messages.send(sender, "admin.setjob.tier-not-supported", "tier", args[1]);
            return;
        }
        JobService.Result result = jobs.forceSetBase(rpgPlayer, args[2]);
        if (result != JobService.Result.OK) {
            messages.send(sender, "admin.setjob.unknown-job", "job", args[2]);
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
            return List.of(TIER_BASE);
        }
        if (args.length == 3 && TIER_BASE.equalsIgnoreCase(args[1])) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            List<String> ids = new ArrayList<>();
            for (JobDefinition job : jobs.tree().baseJobs()) {
                if (job.id().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    ids.add(job.id());
                }
            }
            return ids;
        }
        return List.of();
    }
}
