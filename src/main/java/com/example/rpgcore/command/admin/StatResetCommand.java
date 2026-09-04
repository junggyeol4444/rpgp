package com.example.rpgcore.command.admin;

import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.stat.StatService;
import com.example.rpgcore.util.CommandUtil;
import com.example.rpgcore.util.Messages;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /rpg admin statreset &lt;player&gt; — 스탯 강제 초기화.
 *
 * <p>플레이어가 직접 하는 초기화와 달리 비용을 받지 않고,
 * 초기화 횟수도 올리지 않는다.
 */
public final class StatResetCommand implements AdminSubCommand {

    private final PlayerManager players;
    private final StatService stats;
    private final Messages messages;

    public StatResetCommand(PlayerManager players, StatService stats, Messages messages) {
        this.players = players;
        this.stats = stats;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "statreset";
    }

    @Override
    public String descriptionKey() {
        return "command.admin.statreset.desc";
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
        stats.forceReset(rpgPlayer);
        messages.send(sender, "admin.statreset.done", "player", target.getName());
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        return args.length <= 1
                ? players.onlinePlayers().stream().map(p -> p.player().getName()).toList()
                : List.of();
    }
}
