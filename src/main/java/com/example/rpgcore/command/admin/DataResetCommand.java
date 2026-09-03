package com.example.rpgcore.command.admin;

import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.util.CommandUtil;
import com.example.rpgcore.util.Messages;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /rpg admin datareset &lt;player&gt; — 플레이어 데이터 전체 초기화.
 *
 * <p>되돌릴 수 없는 조작이라 즉시 저장한다.
 * (지시서 5장 [저장 정책] / 기획서 9장)
 */
public final class DataResetCommand implements AdminSubCommand {

    private final PlayerManager players;
    private final Messages messages;

    public DataResetCommand(PlayerManager players, Messages messages) {
        this.players = players;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "datareset";
    }

    @Override
    public String descriptionKey() {
        return "command.admin.datareset.desc";
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
        players.resetData(target);
        messages.send(sender, "admin.datareset.done", "player", target.getName());
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        return args.length <= 1
                ? players.onlinePlayers().stream().map(p -> p.player().getName()).toList()
                : List.of();
    }
}
