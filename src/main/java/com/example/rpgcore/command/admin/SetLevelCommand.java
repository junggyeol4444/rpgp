package com.example.rpgcore.command.admin;

import com.example.rpgcore.level.CombatLevelService;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.util.CommandUtil;
import com.example.rpgcore.util.Messages;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

/** /rpg admin setlevel &lt;player&gt; &lt;level&gt; — 전투 레벨 설정. */
public final class SetLevelCommand implements AdminSubCommand {

    private final PlayerManager players;
    private final CombatLevelService levels;
    private final Messages messages;

    public SetLevelCommand(PlayerManager players, CombatLevelService levels, Messages messages) {
        this.players = players;
        this.levels = levels;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "setlevel";
    }

    @Override
    public String descriptionKey() {
        return "command.admin.setlevel.desc";
    }

    @Override
    public String argHint() {
        return "<player> <level>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messages.send(sender, "common.usage", "usage", usage());
            return;
        }
        Player target = CommandUtil.onlinePlayer(args[0]);
        RpgPlayer rpgPlayer = target == null ? null : players.get(target);
        if (rpgPlayer == null) {
            messages.send(sender, "common.unknown-player", "name", args[0]);
            return;
        }
        Integer level = CommandUtil.parseInt(args[1]);
        if (level == null) {
            messages.send(sender, "common.invalid-number", "input", args[1]);
            return;
        }
        levels.setLevel(rpgPlayer.data(), level);
        messages.send(sender, "admin.setlevel.done",
                "player", target.getName(),
                "level", rpgPlayer.data().combat().level());
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        return args.length <= 1 ? onlineNames() : List.of();
    }

    private List<String> onlineNames() {
        return players.onlinePlayers().stream().map(p -> p.player().getName()).toList();
    }
}
