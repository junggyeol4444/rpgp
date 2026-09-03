package com.example.rpgcore.command.sub;

import com.example.rpgcore.command.SubCommand;
import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.quest.QuestService;
import com.example.rpgcore.ui.gui.GuiManager;
import com.example.rpgcore.ui.gui.QuestGui;
import com.example.rpgcore.util.Messages;
import com.example.rpgcore.util.PluginIds;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** 지시서 12장 — /rpg quest, 퀘스트 목록 GUI. */
public final class QuestCommand implements SubCommand {

    private final ConfigManager config;
    private final PlayerManager players;
    private final QuestService quests;
    private final GuiManager guis;
    private final Messages messages;

    public QuestCommand(ConfigManager config, PlayerManager players, QuestService quests,
                        GuiManager guis, Messages messages) {
        this.config = config;
        this.players = players;
        this.quests = quests;
        this.guis = guis;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "quest";
    }

    @Override
    public String permission() {
        return PluginIds.commandPermission("quest");
    }

    @Override
    public String descriptionKey() {
        return "command.quest.desc";
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
        if (quests.definitions().isEmpty()) {
            messages.send(sender, "command.quest.no-quests");
            return;
        }
        guis.open(rpgPlayer, new QuestGui(config.guiScreen("quest"), quests, messages, guis));
    }
}
