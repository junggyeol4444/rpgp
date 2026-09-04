package com.example.rpgcore.command.admin;

import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.skill.SkillDefinition;
import com.example.rpgcore.skill.SkillService;
import com.example.rpgcore.util.CommandUtil;
import com.example.rpgcore.util.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /rpg admin skill &lt;player&gt; &lt;unlock|remove&gt; &lt;skillId&gt; */
public final class SkillAdminCommand implements AdminSubCommand {

    private final PlayerManager players;
    private final SkillService skills;
    private final Messages messages;

    public SkillAdminCommand(PlayerManager players, SkillService skills, Messages messages) {
        this.players = players;
        this.skills = skills;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "skill";
    }

    @Override
    public String descriptionKey() {
        return "command.admin.skill.desc";
    }

    @Override
    public String argHint() {
        return "<player> <unlock|remove> <skillId>";
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
        SkillService.Result result;
        if (mode.equals("unlock")) {
            result = skills.forceUnlock(rpgPlayer, args[2]);
        } else if (mode.equals("remove")) {
            result = skills.forceRemove(rpgPlayer, args[2]);
        } else {
            messages.send(sender, "common.usage", "usage", usage());
            return;
        }
        if (result != SkillService.Result.OK) {
            messages.send(sender, "admin.skill.failed", "skill", args[2]);
            return;
        }
        messages.send(sender, "admin.skill." + mode,
                "player", target.getName(), "skill", args[2]);
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            return players.onlinePlayers().stream().map(p -> p.player().getName()).toList();
        }
        if (args.length == 2) {
            return List.of("unlock", "remove");
        }
        if (args.length == 3) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            List<String> ids = new ArrayList<>();
            for (SkillDefinition skill : skills.tree().all()) {
                if (skill.id().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    ids.add(skill.id());
                }
            }
            return ids;
        }
        return List.of();
    }
}
