package com.example.rpgcore.command.admin;

import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.stat.StatService;
import com.example.rpgcore.util.CommandUtil;
import com.example.rpgcore.util.Messages;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /rpg admin statpoint &lt;player&gt; &lt;give|take&gt; &lt;amount&gt; */
public final class StatPointCommand implements AdminSubCommand {

    private final PlayerManager players;
    private final StatService stats;
    private final Messages messages;

    public StatPointCommand(PlayerManager players, StatService stats, Messages messages) {
        this.players = players;
        this.stats = stats;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "statpoint";
    }

    @Override
    public String descriptionKey() {
        return "command.admin.statpoint.desc";
    }

    @Override
    public String argHint() {
        return "<player> <give|take> <amount>";
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
        Integer amount = CommandUtil.parseInt(args[2]);
        if (amount == null || amount < 0) {
            messages.send(sender, "common.invalid-number", "input", args[2]);
            return;
        }

        String mode = args[1].toLowerCase(Locale.ROOT);
        if (mode.equals("give")) {
            stats.addPoints(rpgPlayer, amount);
            messages.send(sender, "admin.statpoint.given",
                    "player", target.getName(), "amount", amount);
        } else if (mode.equals("take")) {
            stats.addPoints(rpgPlayer, -amount);
            messages.send(sender, "admin.statpoint.taken",
                    "player", target.getName(), "amount", amount);
        } else {
            messages.send(sender, "common.usage", "usage", usage());
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            return players.onlinePlayers().stream().map(p -> p.player().getName()).toList();
        }
        return args.length == 2 ? List.of("give", "take") : List.of();
    }
}
