package com.example.rpgcore.ui;

import com.example.rpgcore.core.Lifecycle;
import com.example.rpgcore.core.MainThreadExecutor;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 지시서 10장 [상시 표시] 를 한 곳에서 돌린다.
 *
 * <p>채널 하나가 터져도 나머지 표시는 계속 돌아야 하므로 각 채널 호출을
 * 따로 감싼다. 같은 채널이 계속 터지면 그 채널만 꺼버린다.
 */
public final class HudService implements Lifecycle {

    /** 한 채널이 이만큼 연속으로 터지면 끈다. */
    private static final int FAILURE_LIMIT = 5;

    private final PlayerManager players;
    private final MainThreadExecutor mainThread;
    private final Logger logger;
    private final long periodTicks;
    private final Set<String> enabledIds;

    private final List<HudChannel> channels = new ArrayList<>();
    private final List<Integer> failures = new ArrayList<>();
    private MainThreadExecutor.Cancellable task;

    public HudService(PlayerManager players, MainThreadExecutor mainThread, Logger logger,
                      long periodTicks, Set<String> enabledIds) {
        this.players = players;
        this.mainThread = mainThread;
        this.logger = logger;
        this.periodTicks = Math.max(1, periodTicks);
        this.enabledIds = Set.copyOf(enabledIds);
    }

    @Override
    public String serviceName() {
        return "HudService";
    }

    /** config.yml 에서 켜져 있는 채널만 등록된다. */
    public HudService register(HudChannel channel) {
        if (!enabledIds.contains(channel.id())) {
            logger.info("HUD 채널이 꺼져 있어 등록하지 않습니다: " + channel.id());
            return this;
        }
        channels.add(channel);
        failures.add(0);
        return this;
    }

    @Override
    public void enable() {
        task = mainThread.runTimer(this::tick, periodTicks);
    }

    @Override
    public void disable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (RpgPlayer rpgPlayer : players.onlinePlayers()) {
            detach(rpgPlayer);
        }
    }

    public void attach(RpgPlayer rpgPlayer) {
        forEachChannel(rpgPlayer, (channel, target) -> channel.attach(target));
    }

    public void detach(RpgPlayer rpgPlayer) {
        forEachChannel(rpgPlayer, (channel, target) -> channel.detach(target));
    }

    private void tick() {
        for (RpgPlayer rpgPlayer : players.onlinePlayers()) {
            if (rpgPlayer.isOnline()) {
                forEachChannel(rpgPlayer, (channel, target) -> channel.update(target));
            }
        }
    }

    private void forEachChannel(RpgPlayer rpgPlayer, ChannelAction action) {
        for (int i = 0; i < channels.size(); i++) {
            if (failures.get(i) >= FAILURE_LIMIT) {
                continue;
            }
            HudChannel channel = channels.get(i);
            try {
                action.run(channel, rpgPlayer);
                failures.set(i, 0);
            } catch (RuntimeException | LinkageError e) {
                int count = failures.get(i) + 1;
                failures.set(i, count);
                logger.log(Level.WARNING, "HUD 채널 오류: " + channel.id()
                        + (count >= FAILURE_LIMIT ? " (이 채널을 끕니다)" : ""), e);
            }
        }
    }

    @FunctionalInterface
    private interface ChannelAction {
        void run(HudChannel channel, RpgPlayer rpgPlayer);
    }
}
