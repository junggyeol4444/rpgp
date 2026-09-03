package com.example.rpgcore.storage.dirty;

import com.example.rpgcore.config.schema.StorageSettings;
import com.example.rpgcore.core.Lifecycle;
import com.example.rpgcore.core.MainThreadExecutor;
import com.example.rpgcore.player.data.PlayerData;
import com.example.rpgcore.player.data.PlayerDataCodec;
import com.example.rpgcore.storage.PlayerDataRepository;
import com.example.rpgcore.storage.cache.PlayerDataCache;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 지시서 5장 [저장 정책] — 저장 시점과 스레드를 한 곳에서 관리한다.
 *
 * <p>지시서 0장 4번에 따라 파일 입출력은 전부 이 클래스가 만든 IO
 * 스레드에서 일어난다. 메인 스레드에서 디스크에 닿는 경우는 서버 종료
 * 시 마지막 동기 저장뿐이다.
 *
 * <p>[스냅숏을 뜨는 이유]
 * 비동기로 저장하는 동안 메인 스레드가 같은 {@link PlayerData} 를 계속
 * 고칠 수 있다. 그래서 저장을 요청한 스레드에서 먼저 값 사본을 뜨고,
 * IO 스레드에는 그 사본만 넘긴다. 사본 뜨기는 Map 복사라 디스크에
 * 닿지 않는다.
 */
public final class SaveScheduler implements Lifecycle {

    private final PlayerDataRepository repository;
    private final PlayerDataCache cache;
    private final DirtyTracker tracker;
    private final MainThreadExecutor mainThread;
    private final Logger logger;

    private volatile StorageSettings settings = StorageSettings.defaults();

    private ExecutorService io;
    private ScheduledExecutorService ticker;
    private ScheduledFuture<?> autoSaveTask;

    public SaveScheduler(PlayerDataRepository repository,
                         PlayerDataCache cache,
                         DirtyTracker tracker,
                         MainThreadExecutor mainThread,
                         Logger logger) {
        this.repository = repository;
        this.cache = cache;
        this.tracker = tracker;
        this.mainThread = mainThread;
        this.logger = logger;
    }

    @Override
    public String serviceName() {
        return "SaveScheduler";
    }

    /**
     * 설정을 반영한다.
     *
     * <p>주기 저장 간격은 곧바로 반영된다. IO 스레드 수는 이미 만들어진
     * 스레드 풀을 갈아끼워야 해서 서버를 다시 켤 때 반영된다.
     */
    public void applySettings(StorageSettings settings) {
        StorageSettings previous = this.settings;
        this.settings = settings;
        if (ticker != null
                && previous.autoSaveIntervalSeconds() != settings.autoSaveIntervalSeconds()) {
            restartTicker();
        }
        if (io != null && previous.ioThreads() != settings.ioThreads()) {
            logger.info("storage.ioThreads 변경은 서버를 다시 켤 때 반영됩니다.");
        }
    }

    @Override
    public void enable() {
        int threads = Math.max(1, settings.ioThreads());
        io = Executors.newFixedThreadPool(threads, namedFactory("RpgCore-IO"));
        ticker = Executors.newSingleThreadScheduledExecutor(namedFactory("RpgCore-AutoSave"));
        restartTicker();
    }

    @Override
    public void disable() {
        // 지시서 5장: 동기 저장은 서버 종료 시에만 허용한다.
        if (autoSaveTask != null) {
            autoSaveTask.cancel(false);
            autoSaveTask = null;
        }
        if (ticker != null) {
            ticker.shutdownNow();
            ticker = null;
        }
        int saved = 0;
        for (PlayerData data : cache.all()) {
            if (data.isDirty() || tracker.isDirty(data.uuid())) {
                try {
                    repository.save(snapshot(data));
                    data.clearDirty();
                    tracker.clear(data.uuid());
                    saved++;
                } catch (RuntimeException e) {
                    logger.log(Level.SEVERE, "종료 저장에 실패했습니다: " + data.uuid(), e);
                }
            }
        }
        if (io != null) {
            io.shutdown();
            try {
                if (!io.awaitTermination(30, TimeUnit.SECONDS)) {
                    logger.warning("저장 스레드가 30초 안에 끝나지 않아 강제 종료합니다.");
                    io.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                io.shutdownNow();
            }
            io = null;
        }
        repository.saveAll();
        logger.info("종료 저장 완료: " + saved + "명");
    }

    // ------------------------------------------------------------
    // 로드
    // ------------------------------------------------------------

    /**
     * 비동기로 읽는다. 결과를 받아 캐시에 올리는 일은 호출한 쪽이
     * 메인 스레드에서 처리한다. (지시서 5장 [YAML 구현] 로드 항목)
     */
    public CompletableFuture<PlayerData> loadAsync(UUID uuid) {
        ExecutorService executor = io;
        if (executor == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("저장 스케줄러가 아직 켜지지 않았습니다."));
        }
        return CompletableFuture.supplyAsync(() -> repository.load(uuid), executor);
    }

    /** 비동기 작업이 끝난 뒤 메인 스레드로 돌아가기 위한 통로. */
    public MainThreadExecutor mainThread() {
        return mainThread;
    }

    /**
     * 디스크에 닿는 작업을 IO 스레드에서 돌린다.
     *
     * <p>플러그인에서 파일을 읽고 쓰는 스레드는 이 풀 하나뿐이다.
     * 설정 리로드처럼 저장이 아닌 파일 읽기도 여기를 거친다.
     * (지시서 0장 4번)
     */
    public CompletableFuture<Void> runIo(Runnable task) {
        ExecutorService executor = io;
        if (executor == null) {
            task.run();
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(task, executor);
    }

    // ------------------------------------------------------------
    // 저장
    // ------------------------------------------------------------

    /**
     * 값이 바뀌었음을 알린다. 우선순위에 따라 즉시 저장하거나
     * 지연 저장 목록에 올린다.
     */
    public void markDirty(PlayerData data, SavePriority priority) {
        data.markDirty();
        if (priority == SavePriority.IMMEDIATE) {
            submit(data);
        } else {
            tracker.mark(data.uuid());
        }
    }

    /** 지연 저장 목록을 비우면서 전부 저장 큐에 넣는다. */
    public int flushDeferred() {
        Set<UUID> targets = tracker.drain();
        int queued = 0;
        for (UUID uuid : targets) {
            PlayerData data = cache.get(uuid);
            if (data == null) {
                continue;
            }
            submit(data);
            queued++;
        }
        return queued;
    }

    /** 접속 중인 전원을 저장 큐에 넣는다. (/rpg admin save) */
    public int saveAll() {
        tracker.drain();
        int queued = 0;
        for (PlayerData data : cache.all()) {
            submit(data);
            queued++;
        }
        return queued;
    }

    /** 퇴장 처리. 저장이 끝난 뒤 캐시에서 내린다. */
    public CompletableFuture<Void> saveAndRelease(PlayerData data) {
        tracker.clear(data.uuid());
        return submit(data).thenRun(() -> cache.remove(data.uuid()));
    }

    private CompletableFuture<Void> submit(PlayerData data) {
        ExecutorService executor = io;
        PlayerData snapshot = snapshot(data);
        data.clearDirty();
        if (executor == null) {
            // 아직 켜지지 않았거나 이미 내려간 상태. 유실을 막기 위해
            // 이 경우에만 호출 스레드에서 처리한다.
            repository.save(snapshot);
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> repository.save(snapshot), executor)
                .exceptionally(error -> {
                    logger.log(Level.SEVERE, "저장에 실패했습니다: " + snapshot.uuid(), error);
                    return null;
                });
    }

    /**
     * 저장용 사본. 호출한 스레드에서 값 사본을 떠 두면 IO 스레드가
     * 쓰는 동안 원본이 바뀌어도 영향을 받지 않는다.
     */
    private static PlayerData snapshot(PlayerData data) {
        return PlayerDataCodec.fromMap(data.uuid(), PlayerDataCodec.toMap(data), null);
    }

    private void restartTicker() {
        ScheduledExecutorService current = ticker;
        if (current == null) {
            return;
        }
        if (autoSaveTask != null) {
            // 간격이 바뀌면 기존 예약을 반드시 먼저 끊는다.
            // 끊지 않으면 예약이 겹쳐 쌓인다.
            autoSaveTask.cancel(false);
        }
        int seconds = Math.max(10, settings.autoSaveIntervalSeconds());
        autoSaveTask = current.scheduleAtFixedRate(
                () -> mainThread.run(this::flushDeferred),
                seconds, seconds, TimeUnit.SECONDS);
    }

    private static ThreadFactory namedFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }
}
