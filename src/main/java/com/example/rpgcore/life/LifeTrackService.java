package com.example.rpgcore.life;

import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.config.schema.CurveSettings;
import com.example.rpgcore.config.schema.LevelSettings;
import com.example.rpgcore.config.validation.ValidationReport;
import com.example.rpgcore.core.Lifecycle;
import com.example.rpgcore.core.Reloadable;
import com.example.rpgcore.level.ExpCurve;
import com.example.rpgcore.life.unlock.TrackReward;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.player.data.PlayerData;
import com.example.rpgcore.storage.dirty.SavePriority;
import com.example.rpgcore.storage.dirty.SaveScheduler;
import com.example.rpgcore.util.Messages;
import java.util.ArrayList;
import java.util.List;

/**
 * 지시서 3장 [life/LifeTrackService] — 생활 4트랙.
 *
 * <p>기획서 3장: 전투 레벨과 별개로 각 트랙이 독립 레벨을 가진다.
 * 상한은 없고, 레벨업 보상으로 효율 상승과 해금을 모두 준다.
 *
 * <p>트랙 경험치와 레벨업은 지연 저장 대상이다.
 * 해금 id 가 늘어나는 것도 같은 저장에 함께 실린다.
 * (지시서 5장 [저장 정책])
 */
public final class LifeTrackService implements Lifecycle, Reloadable {

    /** 한 번에 처리할 레벨업 횟수 상한. 곡선이 망가져도 서버가 멈추지 않게 한다. */
    private static final int MAX_LEVELUPS_PER_CALL = 10_000;

    private final ConfigManager config;
    private final SaveScheduler saves;
    private final Messages messages;

    private volatile ExpCurve curve = new ExpCurve.Exponential(50.0, 1.10);

    public LifeTrackService(ConfigManager config, SaveScheduler saves, Messages messages) {
        this.config = config;
        this.saves = saves;
        this.messages = messages;
    }

    @Override
    public String serviceName() {
        return "LifeTrackService";
    }

    @Override
    public void enable() {
        rebuildCurve(null);
    }

    @Override
    public void reload(ValidationReport report) {
        rebuildCurve(report);
    }

    private void rebuildCurve(ValidationReport report) {
        curve = ExpCurve.from(config.levels().lifeCurve(), CurveSettings.defaultLife(),
                "levels.yml", "life.curve", report);
    }

    /** 지정한 트랙 레벨에서 다음 레벨로 가는 데 필요한 경험치. */
    public double requiredExp(int level) {
        return curve.requiredExp(level);
    }

    /** 상한에 닿았는지. 상한이 없으면 항상 false. */
    public boolean isMaxLevel(PlayerData data, TrackType type) {
        LevelSettings settings = config.levels();
        return settings.lifeMaxLevel() > 0
                && data.life().track(type).level() >= settings.lifeMaxLevel();
    }

    /** 트랙 정의. 없으면 null. */
    public TrackDefinition definition(TrackType type) {
        return config.life().track(type);
    }

    public TrackReward reward(TrackType type) {
        return config.life().reward(type);
    }

    /** 지금 이 플레이어의 효율값. */
    public double efficiency(PlayerData data, TrackType type, String name) {
        return reward(type).efficiency(name, data.life().track(type).level());
    }

    // ------------------------------------------------------------
    // 획득
    // ------------------------------------------------------------

    /**
     * 획득원에 걸리는 트랙을 찾아 경험치를 준다.
     *
     * <p>같은 획득원을 여러 트랙이 볼 수 있다. (예: BLOCK_BREAK 를
     * 생활과 채광이 함께 본다) 각 트랙은 자기 표에 그 대상이 있을 때만
     * 오른다.
     *
     * @param key 대상 이름. 블록 · 아이템 종류 등. 없으면 null
     * @param multiplier 개수 배수. 1 이상
     */
    public void grant(RpgPlayer rpgPlayer, LifeSource source, String key, int multiplier) {
        if (multiplier <= 0) {
            return;
        }
        for (TrackType type : TrackType.values()) {
            TrackDefinition definition = definition(type);
            if (definition == null || !definition.handles(source)) {
                continue;
            }
            double amount = definition.exp(source, key);
            if (amount > 0) {
                addExp(rpgPlayer, type, amount * multiplier);
            }
        }
    }

    /**
     * 트랙 경험치를 준다.
     *
     * @return 오른 레벨 수
     */
    public int addExp(RpgPlayer rpgPlayer, TrackType type, double amount) {
        if (amount <= 0 || !Double.isFinite(amount)) {
            return 0;
        }
        PlayerData data = rpgPlayer.data();
        PlayerData.Track track = data.life().track(type);
        if (isMaxLevel(data, type)) {
            return 0;
        }

        int before = track.level();
        track.exp(track.exp() + amount);

        LevelSettings settings = config.levels();
        int loops = 0;
        while (loops++ < MAX_LEVELUPS_PER_CALL) {
            if (settings.lifeMaxLevel() > 0 && track.level() >= settings.lifeMaxLevel()) {
                track.exp(0);
                break;
            }
            double required = curve.requiredExp(track.level());
            if (!(required > 0) || !Double.isFinite(required) || track.exp() < required) {
                break;
            }
            track.exp(track.exp() - required);
            track.level(track.level() + 1);
        }

        int after = track.level();
        if (after > before) {
            applyUnlocks(rpgPlayer, type, after);
            messages.send(rpgPlayer.player(), "life.level-up",
                    "track", displayOf(type), "before", before, "after", after);
        }
        saves.markDirty(data, SavePriority.DEFERRED);
        return after - before;
    }

    /** 레벨을 직접 지정한다. (/rpg admin life) */
    public void setLevel(RpgPlayer rpgPlayer, TrackType type, int level) {
        PlayerData data = rpgPlayer.data();
        int clamped = Math.max(1, level);
        LevelSettings settings = config.levels();
        if (settings.lifeMaxLevel() > 0) {
            clamped = Math.min(clamped, settings.lifeMaxLevel());
        }
        data.life().track(type).level(clamped);
        data.life().track(type).exp(0);
        applyUnlocks(rpgPlayer, type, clamped);
        saves.markDirty(data, SavePriority.DEFERRED);
    }

    /** 경험치를 직접 더하거나 뺀다. 레벨업 판정은 하지 않는다. */
    public void setExp(RpgPlayer rpgPlayer, TrackType type, double exp) {
        rpgPlayer.data().life().track(type).exp(Math.max(0, exp));
        saves.markDirty(rpgPlayer.data(), SavePriority.DEFERRED);
    }

    // ------------------------------------------------------------
    // 해금
    // ------------------------------------------------------------

    /**
     * 지금 레벨까지 열리는 것을 전부 채운다.
     *
     * <p>레벨을 건너뛰거나 나중에 설정이 바뀌어도 빠지는 것이 없도록
     * 이하 전부를 훑는다.
     */
    private void applyUnlocks(RpgPlayer rpgPlayer, TrackType type, int level) {
        List<String> unlocked = rpgPlayer.data().life().unlocked();
        List<String> added = new ArrayList<>();
        for (String id : reward(type).unlockedUpTo(level)) {
            if (!unlocked.contains(id)) {
                unlocked.add(id);
                added.add(id);
            }
        }
        for (String id : added) {
            messages.send(rpgPlayer.player(), "life.unlocked", "id", id);
        }
    }

    /** 접속할 때 한 번. 설정이 바뀌어 열려야 할 것이 늘었을 수 있다. */
    public void refreshUnlocks(RpgPlayer rpgPlayer) {
        for (TrackType type : TrackType.values()) {
            applyUnlocks(rpgPlayer, type, rpgPlayer.data().life().track(type).level());
        }
    }

    /** 표시 이름. 정의가 없으면 설정 키를 그대로 준다. */
    public String displayOf(TrackType type) {
        TrackDefinition definition = definition(type);
        return definition == null ? type.configKey() : definition.display();
    }
}
