package com.example.rpgcore.command.admin;

import com.example.rpgcore.job.JobService;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.util.CommandUtil;
import com.example.rpgcore.util.Messages;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /rpg admin jobreset &lt;player&gt; — 전직 상태 초기화. */
public final class JobResetCommand implements AdminSubCommand {

    private final PlayerManager players;
    private final JobService jobs;
    private final Messages messages;

    public JobResetCommand(PlayerManager players, JobService jobs, Messages messages) {
        this.players = players;
        this.jobs = jobs;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "jobreset";
    }

    @Override
    public String descriptionKey() {
        return "command.admin.jobreset.desc";
    }

    @Override
    public String argHint() {
        return "<player>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            messages.send(sender, "common.usage", "usage", usage());
            return;
        }
        Player target = CommandUtil.onlinePlayer(args[0]);
        RpgPlayer rpgPlayer = target == null ? null : players.get(target);
        if (rpgPlayer == null) {
            messages.send(sender, "common.unknown-player", "name", args[0]);
            return;
        }
        jobs.reset(rpgPlayer);
        messages.send(sender, "admin.jobreset.done", "player", target.getName());
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        return args.length <= 1
                ? players.onlinePlayers().stream().map(p -> p.player().getName()).toList()
                : List.of();
    }
}
