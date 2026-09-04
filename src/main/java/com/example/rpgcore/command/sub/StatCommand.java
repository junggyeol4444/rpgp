package com.example.rpgcore.command.sub;

import com.example.rpgcore.command.SubCommand;
import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.stat.StatService;
import com.example.rpgcore.ui.gui.GuiManager;
import com.example.rpgcore.ui.gui.StatGui;
import com.example.rpgcore.util.Messages;
import com.example.rpgcore.util.PluginIds;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** 지시서 12장 — /rpg stat, 스탯 분배 GUI. */
public final class StatCommand implements SubCommand {

    private final ConfigManager config;
    private final PlayerManager players;
    private final StatService stats;
    private final GuiManager guis;
    private final Messages messages;

    public StatCommand(ConfigManager config, PlayerManager players, StatService stats,
                       GuiManager guis, Messages messages) {
        this.config = config;
        this.players = players;
        this.stats = stats;
        this.guis = guis;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "stat";
    }

    @Override
    public String permission() {
        return PluginIds.commandPermission("stat");
    }

    @Override
    public String descriptionKey() {
        return "command.stat.desc";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        RpgPlayer rpgPlayer = players.get((Player) sender);
        if (rpgPlayer == null) {
            messages.send(sender, "common.data-not-loaded");
            return;
        }
        if (stats.settings().stats().isEmpty()) {
            messages.send(sender, "command.stat.no-stats");
            return;
        }
        // 화면마다 자기 인벤토리와 슬롯 표를 들고 있으므로 열 때마다 새로 만든다.
        guis.open(rpgPlayer, new StatGui(config.guiScreen("stat"), stats, messages, guis));
    }
}
