package com.example.rpgcore.command.admin;

import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.quest.QuestDefinition;
import com.example.rpgcore.quest.QuestService;
import com.example.rpgcore.util.CommandUtil;
import com.example.rpgcore.util.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /rpg admin quest &lt;player&gt; &lt;give|complete|cancel&gt; &lt;questId&gt; */
public final class QuestAdminCommand implements AdminSubCommand {

    private final PlayerManager players;
    private final QuestService quests;
    private final Messages messages;

    public QuestAdminCommand(PlayerManager players, QuestService quests, Messages messages) {
        this.players = players;
        this.quests = quests;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "quest";
    }

    @Override
    public String descriptionKey() {
        return "command.admin.quest.desc";
    }

    @Override
    public String argHint() {
        return "<player> <give|complete|cancel> <questId>";
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
        String mode = args[1].toLowerCase(Locale.ROOT);
        String questId = args[2];

        QuestService.Result result = switch (mode) {
            case "give" -> quests.accept(rpgPlayer, questId);
            case "complete" -> quests.forceComplete(rpgPlayer, questId);
            case "cancel" -> quests.abandon(rpgPlayer, questId);
            default -> null;
        };
        if (result == null) {
            messages.send(sender, "common.usage", "usage", usage());
            return;
        }
        if (result != QuestService.Result.OK) {
            messages.send(sender, "quest.result."
                    + result.name().toLowerCase(Locale.ROOT).replace('_', '-'));
            return;
        }
        messages.send(sender, "admin.quest." + mode,
                "player", target.getName(), "quest", questId);
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            return players.onlinePlayers().stream().map(p -> p.player().getName()).toList();
        }
        if (args.length == 2) {
            return List.of("give", "complete", "cancel");
        }
        if (args.length == 3) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            List<String> ids = new ArrayList<>();
            for (QuestDefinition quest : quests.definitions().values()) {
                if (quest.id().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    ids.add(quest.id());
                }
            }
            return ids;
        }
        return List.of();
    }
}
