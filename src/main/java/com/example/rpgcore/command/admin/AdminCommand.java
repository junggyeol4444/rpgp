package com.example.rpgcore.command.admin;

import com.example.rpgcore.command.SubCommand;
import com.example.rpgcore.util.Messages;
import com.example.rpgcore.util.PluginIds;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.CommandSender;

/**
 * 지시서 12장 [관리자 명령] — /rpg admin 의 분기점.
 *
 * <p>지시서 14장 [병행 작업]: 관리자 명령은 각 단계에서 해당 기능이
 * 생길 때 같이 추가한다. 그래서 지금 등록된 것은 1단계 범위
 * (플러그인 운영 + 전투 레벨·경험치 + 플레이어 데이터)뿐이다.
 */
public final class AdminCommand implements SubCommand {

    private final Messages messages;
    private final Map<String, AdminSubCommand> children = new LinkedHashMap<>();

    public AdminCommand(Messages messages) {
        this.messages = messages;
    }

    public AdminCommand register(AdminSubCommand child) {
        children.put(child.name().toLowerCase(Locale.ROOT), child);
        return this;
    }

    @Override
    public String name() {
        return "admin";
    }

    @Override
    public String permission() {
        // 하위 명령 각각의 권한은 따로 검사한다.
        return PluginIds.PERMISSION_PREFIX + "admin";
    }

    @Override
    public String descriptionKey() {
        return "command.admin.desc";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }
        AdminSubCommand target = children.get(args[0].toLowerCase(Locale.ROOT));
        if (target == null) {
            messages.send(sender, "common.unknown-subcommand", "input", args[0]);
            return;
        }
        if (!sender.hasPermission(target.permission())) {
            messages.send(sender, "common.no-permission");
            return;
        }
        target.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            List<String> names = new ArrayList<>();
            for (AdminSubCommand child : children.values()) {
                if (sender.hasPermission(child.permission()) && child.name().startsWith(prefix)) {
                    names.add(child.name());
                }
            }
            return names;
        }
        AdminSubCommand target = children.get(args[0].toLowerCase(Locale.ROOT));
        if (target == null || !sender.hasPermission(target.permission())) {
            return List.of();
        }
        return target.complete(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    private void sendHelp(CommandSender sender) {
        messages.sendPlain(sender, "help.header");
        for (AdminSubCommand child : children.values()) {
            if (!sender.hasPermission(child.permission())) {
                continue;
            }
            messages.sendPlain(sender, "help.line",
                    "usage", child.usage(),
                    "desc", messages.format(child.descriptionKey()));
        }
    }
}
