package com.example.rpgcore.command;

import com.example.rpgcore.util.Messages;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * 지시서 12장 — 루트 명령 /rpg.
 *
 * <p>등록된 명령은 이것 하나뿐이고 하위 명령은 여기서 분기한다.
 */
public final class RpgCommand implements CommandExecutor, TabCompleter {

    private final Messages messages;
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public RpgCommand(Messages messages) {
        this.messages = messages;
    }

    /** 하위 명령을 등록한다. 등록 순서대로 도움말에 나온다. */
    public RpgCommand register(SubCommand subCommand) {
        subCommands.put(subCommand.name().toLowerCase(Locale.ROOT), subCommand);
        return this;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        SubCommand target = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (target == null) {
            messages.send(sender, "common.unknown-subcommand", "input", args[0]);
            return true;
        }
        if (!sender.hasPermission(target.permission())) {
            messages.send(sender, "common.no-permission");
            return true;
        }
        if (target.playerOnly() && !(sender instanceof Player)) {
            messages.send(sender, "common.player-only");
            return true;
        }
        target.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String label, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            List<String> names = new ArrayList<>();
            for (SubCommand sub : subCommands.values()) {
                if (sender.hasPermission(sub.permission()) && sub.name().startsWith(prefix)) {
                    names.add(sub.name());
                }
            }
            return names;
        }

        SubCommand target = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (target == null || !sender.hasPermission(target.permission())) {
            return List.of();
        }
        return target.complete(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    private void sendHelp(CommandSender sender) {
        messages.sendPlain(sender, "help.header");
        for (SubCommand sub : subCommands.values()) {
            if (!sender.hasPermission(sub.permission())) {
                continue;
            }
            messages.sendPlain(sender, "help.line",
                    "usage", sub.usage(),
                    "desc", messages.format(sub.descriptionKey()));
        }
    }
}
