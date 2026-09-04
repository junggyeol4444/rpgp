package com.example.rpgcore.command.sub;

import com.example.rpgcore.command.SubCommand;
import com.example.rpgcore.level.CombatLevelService;
import com.example.rpgcore.life.LifeTrackService;
import com.example.rpgcore.life.TrackType;
import com.example.rpgcore.life.unlock.TrackReward;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.player.data.PlayerData;
import com.example.rpgcore.util.Messages;
import com.example.rpgcore.util.PluginIds;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** 지시서 12장 — /rpg life, 생활 트랙 현황. */
public final class LifeCommand implements SubCommand {

    private final PlayerManager players;
    private final LifeTrackService tracks;
    private final Messages messages;

    public LifeCommand(PlayerManager players, LifeTrackService tracks, Messages messages) {
        this.players = players;
        this.tracks = tracks;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "life";
    }

    @Override
    public String permission() {
        return PluginIds.commandPermission("life");
    }

    @Override
    public String descriptionKey() {
        return "command.life.desc";
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
        PlayerData data = rpgPlayer.data();

        messages.sendPlain(sender, "command.life.header");
        for (TrackType type : TrackType.values()) {
            PlayerData.Track track = data.life().track(type);
            messages.sendPlain(sender, "command.life.track",
                    "track", tracks.displayOf(type),
                    "level", track.level(),
                    "exp", CombatLevelService.format(track.exp()),
                    "required", CombatLevelService.format(tracks.requiredExp(track.level())));

            TrackReward reward = tracks.reward(type);
            for (Map.Entry<String, Double> entry : reward.efficiencyPerLevel().entrySet()) {
                messages.sendPlain(sender, "command.life.efficiency",
                        "name", entry.getKey(),
                        "value", format(tracks.efficiency(data, type, entry.getKey())));
            }
        }
        messages.sendPlain(sender, "command.life.unlocked",
                "count", data.life().unlocked().size());
    }

    private static String format(double value) {
        return String.valueOf(Math.round(value * 1000.0) / 1000.0);
    }
}
