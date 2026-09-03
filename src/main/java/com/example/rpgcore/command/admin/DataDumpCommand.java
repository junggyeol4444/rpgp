package com.example.rpgcore.command.admin;

import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.player.data.PlayerDataCodec;
import com.example.rpgcore.util.CommandUtil;
import com.example.rpgcore.util.Messages;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /rpg admin datadump &lt;player&gt; — 현재 값 덤프.
 *
 * <p>저장 파일에 실제로 쓰이는 형태 그대로 보여준다.
 */
public final class DataDumpCommand implements AdminSubCommand {

    private final PlayerManager players;
    private final Messages messages;

    public DataDumpCommand(PlayerManager players, Messages messages) {
        this.players = players;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "datadump";
    }

    @Override
    public String descriptionKey() {
        return "command.admin.datadump.desc";
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

        messages.sendPlain(sender, "admin.datadump.header", "player", target.getName());
        dump(sender, "", PlayerDataCodec.toMap(rpgPlayer.data()));
    }

    private void dump(CommandSender sender, String prefix, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> child) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typed = (Map<String, Object>) child;
                if (typed.isEmpty()) {
                    messages.sendPlain(sender, "admin.datadump.line", "path", path, "value", "{}");
                } else {
                    dump(sender, path, typed);
                }
            } else {
                messages.sendPlain(sender, "admin.datadump.line",
                        "path", path, "value", String.valueOf(value));
            }
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        return args.length <= 1
                ? players.onlinePlayers().stream().map(p -> p.player().getName()).toList()
                : List.of();
    }
}
