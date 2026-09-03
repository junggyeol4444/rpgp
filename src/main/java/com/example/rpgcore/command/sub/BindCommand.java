package com.example.rpgcore.command.sub;

import com.example.rpgcore.binding.BindingService;
import com.example.rpgcore.command.SubCommand;
import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.skill.SkillService;
import com.example.rpgcore.ui.gui.BindGui;
import com.example.rpgcore.ui.gui.GuiManager;
import com.example.rpgcore.util.Messages;
import com.example.rpgcore.util.PluginIds;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** 지시서 12장 — /rpg bind, 스킬 등록 GUI. */
public final class BindCommand implements SubCommand {

    private final ConfigManager config;
    private final PlayerManager players;
    private final BindingService bindings;
    private final SkillService skills;
    private final GuiManager guis;
    private final Messages messages;

    public BindCommand(ConfigManager config, PlayerManager players, BindingService bindings,
                       SkillService skills, GuiManager guis, Messages messages) {
        this.config = config;
        this.players = players;
        this.bindings = bindings;
        this.skills = skills;
        this.guis = guis;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "bind";
    }

    @Override
    public String permission() {
        return PluginIds.commandPermission("bind");
    }

    @Override
    public String descriptionKey() {
        return "command.bind.desc";
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
        if (rpgPlayer.data().skill().unlocked().isEmpty()) {
            messages.send(sender, "command.bind.no-skills");
            return;
        }
        guis.open(rpgPlayer,
                new BindGui(config.guiScreen("bind"), bindings, skills, messages, guis));
    }
}
