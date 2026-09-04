package com.example.rpgcore.command.admin;

import com.example.rpgcore.storage.dirty.SaveScheduler;
import com.example.rpgcore.util.Messages;
import org.bukkit.command.CommandSender;

/** /rpg admin save — 저장 강제 실행. */
public final class SaveCommand implements AdminSubCommand {

    private final SaveScheduler saves;
    private final Messages messages;

    public SaveCommand(SaveScheduler saves, Messages messages) {
        this.saves = saves;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "save";
    }

    @Override
    public String descriptionKey() {
        return "command.admin.save.desc";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        int queued = saves.saveAll();
        messages.send(sender, "admin.save.done", "count", queued);
    }
}
