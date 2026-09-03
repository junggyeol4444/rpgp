package com.example.rpgcore.command.admin;

import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.storage.PlayerDataRepository;
import com.example.rpgcore.util.Messages;
import org.bukkit.command.CommandSender;

/** /rpg admin status — 플러그인 상태·버전 확인. */
public final class StatusCommand implements AdminSubCommand {

    private final String version;
    private final ConfigManager config;
    private final PlayerDataRepository repository;
    private final PlayerManager players;
    private final Messages messages;

    public StatusCommand(String version, ConfigManager config, PlayerDataRepository repository,
                         PlayerManager players, Messages messages) {
        this.version = version;
        this.config = config;
        this.repository = repository;
        this.players = players;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "status";
    }

    @Override
    public String descriptionKey() {
        return "command.admin.status.desc";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        messages.sendPlain(sender, "admin.status.header");
        messages.sendPlain(sender, "admin.status.version", "version", version);
        messages.sendPlain(sender, "admin.status.storage", "type", repository.storageType());
        messages.sendPlain(sender, "admin.status.online", "cached", players.loadedCount());
        messages.sendPlain(sender, "admin.status.autosave",
                "seconds", config.general().storage().autoSaveIntervalSeconds());
        messages.sendPlain(sender, "admin.status.debug",
                "state", messages.format(config.debug() ? "state.enabled" : "state.disabled"));
        messages.sendPlain(sender, "admin.status.config-errors",
                "count", config.lastErrorCount());
    }
}
