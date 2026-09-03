package com.example.rpgcore.command.admin;

import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.quest.QuestService;
import com.example.rpgcore.quest.QuestType;
import com.example.rpgcore.util.Messages;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandSender;

/**
 * /rpg admin questcycle &lt;daily|weekly&gt; reset
 *
 * <p>접속 중인 전원의 해당 주기 완료 이력을 지운다.
 */
public final class QuestCycleCommand implements AdminSubCommand {

    private final PlayerManager players;
    private final QuestService quests;
    private final Messages messages;

    public QuestCycleCommand(PlayerManager players, QuestService quests, Messages messages) {
        this.players = players;
        this.quests = quests;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "questcycle";
    }

    @Override
    public String descriptionKey() {
        return "command.admin.questcycle.desc";
    }

    @Override
    public String argHint() {
        return "<daily|weekly> reset";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("reset")) {
            messages.send(sender, "common.usage", "usage", usage());
            return;
        }
        QuestType type = switch (args[0].toLowerCase(Locale.ROOT)) {
            case "daily" -> QuestType.DAILY;
            case "weekly" -> QuestType.WEEKLY;
            default -> null;
        };
        if (type == null) {
            messages.send(sender, "common.usage", "usage", usage());
            return;
        }

        long now = System.currentTimeMillis();
        int cleared = 0;
        for (RpgPlayer rpgPlayer : players.onlinePlayers()) {
            cleared += quests.resetCycle(rpgPlayer, type, now);
        }
        messages.send(sender, "admin.questcycle.done",
                "type", args[0].toLowerCase(Locale.ROOT),
                "players", players.loadedCount(),
                "count", cleared);
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            return List.of("daily", "weekly");
        }
        return args.length == 2 ? List.of("reset") : List.of();
    }
}
