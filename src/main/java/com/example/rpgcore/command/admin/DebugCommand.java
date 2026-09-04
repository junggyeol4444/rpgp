package com.example.rpgcore.command.admin;

import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.util.Messages;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandSender;

/**
 * /rpg admin debug &lt;on|off&gt; — 디버그 모드 토글.
 *
 * <p>지시서 11장: 켜면 데미지 계산 과정을 로그로 출력한다.
 * 실제 출력은 2단계(커스텀 데미지 계산)에서 붙는다.
 */
public final class DebugCommand implements AdminSubCommand {

    private final ConfigManager config;
    private final Messages messages;

    public DebugCommand(ConfigManager config, Messages messages) {
        this.config = config;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "debug";
    }

    @Override
    public String descriptionKey() {
        return "command.admin.debug.desc";
    }

    @Override
    public String argHint() {
        return "<on|off>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            messages.send(sender, "common.usage", "usage", usage());
            return;
        }
        String value = args[0].toLowerCase(Locale.ROOT);
        boolean enabled;
        if (value.equals("on")) {
            enabled = true;
        } else if (value.equals("off")) {
            enabled = false;
        } else {
            messages.send(sender, "common.usage", "usage", usage());
            return;
        }
        config.debug(enabled);
        messages.send(sender, "admin.debug.toggled",
                "state", messages.format(enabled ? "state.enabled" : "state.disabled"));
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        return args.length <= 1 ? List.of("on", "off") : List.of();
    }
}
