package com.example.rpgcore.command.admin;

import com.example.rpgcore.life.LifeTrackService;
import com.example.rpgcore.life.TrackType;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.util.CommandUtil;
import com.example.rpgcore.util.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /rpg admin life &lt;player&gt; &lt;track&gt; &lt;level|exp&gt; &lt;값&gt; */
public final class LifeAdminCommand implements AdminSubCommand {

    private final PlayerManager players;
    private final LifeTrackService tracks;
    private final Messages messages;

    public LifeAdminCommand(PlayerManager players, LifeTrackService tracks, Messages messages) {
        this.players = players;
        this.tracks = tracks;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "life";
    }

    @Override
    public String descriptionKey() {
        return "command.admin.life.desc";
    }

    @Override
    public String argHint() {
        return "<player> <track> <level|exp> <값>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 4) {
            messages.send(sender, "common.usage", "usage", usage());
            return;
        }
        Player target = CommandUtil.onlinePlayer(args[0]);
        RpgPlayer rpgPlayer = target == null ? null : players.get(target);
        if (rpgPlayer == null) {
            messages.send(sender, "common.unknown-player", "name", args[0]);
            return;
        }
        TrackType type = TrackType.fromConfigKey(args[1]);
        if (type == null) {
            messages.send(sender, "admin.life.unknown-track", "track", args[1]);
            return;
        }
        Double value = CommandUtil.parseDouble(args[3]);
        if (value == null || value < 0) {
            messages.send(sender, "common.invalid-number", "input", args[3]);
            return;
        }

        String field = args[2].toLowerCase(Locale.ROOT);
        if (field.equals("level")) {
            tracks.setLevel(rpgPlayer, type, (int) Math.round(value));
        } else if (field.equals("exp")) {
            tracks.setExp(rpgPlayer, type, value);
        } else {
            messages.send(sender, "common.usage", "usage", usage());
            return;
        }
        messages.send(sender, "admin.life.done",
                "player", target.getName(),
                "track", tracks.displayOf(type),
                "field", field,
                "value", args[3]);
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            return players.onlinePlayers().stream().map(p -> p.player().getName()).toList();
        }
        if (args.length == 2) {
            List<String> keys = new ArrayList<>();
            for (TrackType type : TrackType.values()) {
                keys.add(type.configKey());
            }
            return keys;
        }
        return args.length == 3 ? List.of("level", "exp") : List.of();
    }
}
