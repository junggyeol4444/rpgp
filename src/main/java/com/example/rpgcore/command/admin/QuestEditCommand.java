package com.example.rpgcore.command.admin;

import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.quest.QuestService;
import com.example.rpgcore.quest.editor.QuestEditorService;
import com.example.rpgcore.ui.gui.GuiManager;
import com.example.rpgcore.ui.gui.QuestEditorGui;
import com.example.rpgcore.util.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 지시서 14장 10단계 — /rpg admin questedit, 퀘스트 GUI 에디터를 연다.
 *
 * <p>화면을 열려면 플레이어여야 한다. 콘솔에서는 쓸 수 없다.
 */
public final class QuestEditCommand implements AdminSubCommand {

    private final ConfigManager config;
    private final PlayerManager players;
    private final QuestService quests;
    private final QuestEditorService editor;
    private final GuiManager guis;
    private final Messages messages;

    public QuestEditCommand(ConfigManager config, PlayerManager players, QuestService quests,
                            QuestEditorService editor, GuiManager guis, Messages messages) {
        this.config = config;
        this.players = players;
        this.quests = quests;
        this.editor = editor;
        this.guis = guis;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "questedit";
    }

    @Override
    public String descriptionKey() {
        return "command.admin.questedit.desc";
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
        // 편집을 새로 시작하므로 남아 있던 초안과 입력 대기를 버린다.
        editor.clearDraft(rpgPlayer);
        editor.clearPrompt(rpgPlayer.uuid());
        guis.open(rpgPlayer, new QuestEditorGui(
                config.guiScreen("questeditor"), config.guiScreen("questedit"),
                quests, editor, messages, guis));
    }
}
