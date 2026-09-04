package com.example.rpgcore.core;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * {@link MainThreadExecutor} 의 Bukkit 스케줄러 구현.
 *
 * <p>[확인 완료] BukkitScheduler 의 runTask · runTaskTimer 는 26.1.2 에
 * 그대로 있고 사용 중단도 아니다. Paper 26.1.2 API 소스로 컴파일해 확인했다. (tools/verify-against-paper.sh)
 * 이 파일은 플러그인 전체에서 스케줄러를 호출하는 유일한 지점이다.
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
