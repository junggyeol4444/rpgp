package com.example.rpgcore.command.admin;

import com.example.rpgcore.level.CombatLevelService;
import com.example.rpgcore.level.ExpSource;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.util.CommandUtil;
import com.example.rpgcore.util.Messages;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /rpg admin exp &lt;player&gt; &lt;give|take&gt; &lt;amount&gt;
 *
 * <p>give 는 레벨업 판정까지 태운다. take 는 경험치 수치만 깎는다.
 */
public final class ExpCommand implements AdminSubCommand {

    private final PlayerManager players;
    private final CombatLevelService levels;
    private final Messages messages;

    public ExpCommand(PlayerManager players, CombatLevelService levels, Messages messages) {
        this.players = players;
        this.levels = levels;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "exp";
    }

    @Override
    public String descriptionKey() {
        return "command.admin.exp.desc";
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
        Double amount = CommandUtil.parseDouble(args[2]);
        if (amount == null || amount < 0) {
            messages.send(sender, "common.invalid-number", "input", args[2]);
            return;
        }

        String mode = args[1].toLowerCase(Locale.ROOT);
        if (mode.equals("give")) {
            levels.addExp(rpgPlayer, amount, ExpSource.ADMIN);
            messages.send(sender, "admin.exp.given",
                    "player", target.getName(), "amount", CombatLevelService.format(amount));
        } else if (mode.equals("take")) {
            levels.addRawExp(rpgPlayer.data(), -amount);
            messages.send(sender, "admin.exp.taken",
                    "player", target.getName(), "amount", CombatLevelService.format(amount));
        } else {
            messages.send(sender, "common.usage", "usage", usage());
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            return players.onlinePlayers().stream().map(p -> p.player().getName()).toList();
        }
        if (args.length == 2) {
            return List.of("give", "take");
        }
        return List.of();
    }
}
