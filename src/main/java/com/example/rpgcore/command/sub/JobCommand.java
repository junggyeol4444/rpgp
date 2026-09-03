package com.example.rpgcore.command.sub;

import com.example.rpgcore.command.SubCommand;
import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.job.JobService;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.ui.gui.GuiManager;
import com.example.rpgcore.ui.gui.JobGui;
import com.example.rpgcore.util.Messages;
import com.example.rpgcore.util.PluginIds;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** 지시서 12장 — /rpg job, 직업 선택·전직 GUI. */
public final class JobCommand implements SubCommand {

    private final ConfigManager config;
    private final PlayerManager players;
    private final JobService jobs;
    private final GuiManager guis;
    private final Messages messages;

    public JobCommand(ConfigManager config, PlayerManager players, JobService jobs,
                      GuiManager guis, Messages messages) {
        this.config = config;
        this.players = players;
        this.jobs = jobs;
        this.guis = guis;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "job";
    }

    @Override
    public String permission() {
        return PluginIds.commandPermission("job");
    }

    @Override
    public String descriptionKey() {
        return "command.job.desc";
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
        if (jobs.tree().size() == 0) {
            messages.send(sender, "command.job.no-jobs");
            return;
        }
        guis.open(rpgPlayer, new JobGui(config.guiScreen("job"), jobs, messages, guis));
    }
}
