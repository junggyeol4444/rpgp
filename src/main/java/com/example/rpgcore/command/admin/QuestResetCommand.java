package com.example.rpgcore.command.admin;

import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.quest.QuestService;
import com.example.rpgcore.util.CommandUtil;
import com.example.rpgcore.util.Messages;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /rpg admin questreset &lt;player&gt; — 퀘스트 진행 이력 초기화. */
public final class QuestResetCommand implements AdminSubCommand {

    private final PlayerManager players;
    private final QuestService quests;
    private final Messages messages;

    public QuestResetCommand(PlayerManager players, QuestService quests, Messages messages) {
        this.players = players;
        this.quests = quests;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "questreset";
    }

    @Override
    public String descriptionKey() {
        return "command.admin.questreset.desc";
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
        quests.resetAll(rpgPlayer);
        messages.send(sender, "admin.questreset.done", "player", target.getName());
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        return args.length <= 1
                ? players.onlinePlayers().stream().map(p -> p.player().getName()).toList()
                : List.of();
    }
}
