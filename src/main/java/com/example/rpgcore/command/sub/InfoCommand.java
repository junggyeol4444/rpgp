package com.example.rpgcore.command.sub;

import com.example.rpgcore.command.SubCommand;
import com.example.rpgcore.job.JobService;
import com.example.rpgcore.level.CombatLevelService;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.player.data.PlayerData;
import com.example.rpgcore.util.Messages;
import com.example.rpgcore.util.PluginIds;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 지시서 12장 — /rpg info, 현재 상태 요약.
 *
 * <p>단계가 올라갈 때마다 줄을 추가한다. (지시서 14장 [병행 작업])
 * 지금은 전투 레벨·경험치·남은 스탯 포인트·직업까지다.
 */
public final class InfoCommand implements SubCommand {

    private final PlayerManager players;
    private final CombatLevelService levels;
    private final JobService jobs;
    private final Messages messages;

    public InfoCommand(PlayerManager players, CombatLevelService levels,
                       JobService jobs, Messages messages) {
        this.players = players;
        this.levels = levels;
        this.jobs = jobs;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "info";
    }

    @Override
    public String permission() {
        return PluginIds.commandPermission("info");
    }

    @Override
    public String descriptionKey() {
        return "command.info.desc";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        RpgPlayer rpgPlayer = players.get(player);
        if (rpgPlayer == null) {
            messages.send(sender, "common.data-not-loaded");
            return;
        }

        PlayerData data = rpgPlayer.data();
        int level = data.combat().level();

        messages.sendPlain(sender, "command.info.header", "player", player.getName());
        messages.sendPlain(sender, "command.info.level", "level", level);
        if (levels.isMaxLevel(data)) {
            messages.sendPlain(sender, "command.info.exp-max",
                    "exp", CombatLevelService.format(data.combat().exp()));
        } else {
            messages.sendPlain(sender, "command.info.exp",
                    "exp", CombatLevelService.format(data.combat().exp()),
                    "required", CombatLevelService.format(levels.requiredExp(level)));
        }
        messages.sendPlain(sender, "command.info.stat-points",
                "points", data.combat().statPoints());

        String job = jobs.displayName(data);
        if (job == null) {
            messages.sendPlain(sender, "command.info.job-none",
                    "level", jobs.settings().jobSelectLevel());
        } else {
            messages.sendPlain(sender, "command.info.job", "job", job);
        }
    }
}
