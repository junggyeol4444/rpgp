package com.example.rpgcore.command.sub;

import com.example.rpgcore.command.SubCommand;
import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.skill.SkillService;
import com.example.rpgcore.ui.gui.GuiManager;
import com.example.rpgcore.ui.gui.SkillTreeGui;
import com.example.rpgcore.util.Messages;
import com.example.rpgcore.util.PluginIds;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** 지시서 12장 — /rpg skill, 스킬트리 GUI. */
public final class SkillCommand implements SubCommand {

    private final ConfigManager config;
    private final PlayerManager players;
    private final SkillService skills;
    private final GuiManager guis;
    private final Messages messages;

    public SkillCommand(ConfigManager config, PlayerManager players, SkillService skills,
                        GuiManager guis, Messages messages) {
        this.config = config;
        this.players = players;
        this.skills = skills;
        this.guis = guis;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "skill";
    }

    @Override
    public String permission() {
        return PluginIds.commandPermission("skill");
    }

    @Override
    public String descriptionKey() {
        return "command.skill.desc";
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
        if (!rpgPlayer.data().job().hasBase()) {
            messages.send(sender, "command.skill.no-job");
            return;
        }
        if (skills.tree().ofJob(rpgPlayer.data().job().base()).isEmpty()) {
            messages.send(sender, "command.skill.no-skills");
            return;
        }
        guis.open(rpgPlayer, new SkillTreeGui(config.guiScreen("skill"), skills, messages, guis));
    }
}
