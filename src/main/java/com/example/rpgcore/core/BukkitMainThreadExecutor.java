package com.example.rpgcore.core;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * {@link MainThreadExecutor} 의 Bukkit 스케줄러 구현.
 *
 * <p>[확인 필요 - 지시서 16장]
 * 26.x 에서 Bukkit 스케줄러 API가 그대로인지 확인한 뒤 확정한다.
 * 이 파일은 플러그인 전체에서 스케줄러를 호출하는 유일한 지점이므로,
 * API가 다르다면 여기만 고치면 된다.
 */
public final class BukkitMainThreadExecutor implements MainThreadExecutor {

    private final Plugin plugin;

    public BukkitMainThreadExecutor(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run(Runnable task) {
        if (isMainThread()) {
            task.run();
            return;
        }
        if (!plugin.isEnabled()) {
            // 종료 중에는 스케줄이 거부된다. 이 경우 넘어가는 쪽이 안전하다.
            return;
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public boolean isMainThread() {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public Cancellable runTimer(Runnable task, long periodTicks) {
        if (!plugin.isEnabled()) {
            return () -> { };
        }
        BukkitTask scheduled =
                Bukkit.getScheduler().runTaskTimer(plugin, task, periodTicks, periodTicks);
        return scheduled::cancel;
    }
}
