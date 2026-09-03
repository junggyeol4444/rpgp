package com.example.rpgcore.job;

import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.config.schema.JobSettings;
import com.example.rpgcore.core.Lifecycle;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.player.data.PlayerData;
import com.example.rpgcore.stat.StatService;
import com.example.rpgcore.storage.dirty.SavePriority;
import com.example.rpgcore.storage.dirty.SaveScheduler;

/**
 * 지시서 3장 [job/JobService] — 직업 선택·전직 처리.
 *
 * <p>3단계 범위는 기본 직업 선택까지다. 1차·2차 전직은 8·9단계다.
 *
 * <p>기본 직업 선택은 되돌릴 수 없으므로 즉시 저장한다.
 * (지시서 5장 [저장 정책] / 기획서 9장)
 *
 * <p>리로드로 직업 보정치가 바뀌었을 때 파생 수치를 다시 계산하는 일은
 * {@link StatService} 가 맡는다. 여기서 또 돌리면 같은 일을 두 번 한다.
 */
public final class JobService implements Lifecycle {

    /** 직업 선택 시도의 결과. */
    public enum Result {
        OK,
        /** jobs.yml 에 직업 정의가 없음 */
        NO_JOBS,
        /** 그런 직업이 없음 */
        UNKNOWN_JOB,
        /** 레벨이 모자람 */
        LEVEL_TOO_LOW,
        /** 이미 골랐고 되돌릴 수 없음 */
        ALREADY_SELECTED
    }

    private final ConfigManager config;
    private final SaveScheduler saves;
    private final StatService stats;

    public JobService(ConfigManager config, SaveScheduler saves, StatService stats) {
        this.config = config;
        this.saves = saves;
        this.stats = stats;
    }

    @Override
    public String serviceName() {
        return "JobService";
    }

    public JobSettings settings() {
        return config.jobs();
    }

    public JobTree tree() {
        return config.jobs().tree();
    }

    /** 지금 고를 수 있는지. 고를 수 있으면 {@link Result#OK}. */
    public Result canSelect(PlayerData data) {
        JobSettings settings = config.jobs();
        if (settings.tree().size() == 0) {
            return Result.NO_JOBS;
        }
        if (data.job().hasBase() && !settings.branchRevert()) {
            return Result.ALREADY_SELECTED;
        }
        if (data.combat().level() < settings.jobSelectLevel()) {
            return Result.LEVEL_TOO_LOW;
        }
        return Result.OK;
    }

    /** 기본 직업을 고른다. */
    public Result select(RpgPlayer rpgPlayer, String jobId) {
        Result check = canSelect(rpgPlayer.data());
        if (check != Result.OK) {
            return check;
        }
        if (!tree().hasBase(jobId)) {
            return Result.UNKNOWN_JOB;
        }
        rpgPlayer.data().job().base(jobId);
        saves.markDirty(rpgPlayer.data(), SavePriority.IMMEDIATE);
        stats.refresh(rpgPlayer);
        return Result.OK;
    }

    /**
     * 관리자용 강제 지정. 레벨과 재선택 제한을 무시한다.
     * (/rpg admin setjob)
     */
    public Result forceSetBase(RpgPlayer rpgPlayer, String jobId) {
        if (!tree().hasBase(jobId)) {
            return Result.UNKNOWN_JOB;
        }
        rpgPlayer.data().job().base(jobId);
        saves.markDirty(rpgPlayer.data(), SavePriority.IMMEDIATE);
        stats.refresh(rpgPlayer);
        return Result.OK;
    }

    /** 관리자용 전직 상태 초기화. (/rpg admin jobreset) */
    public void reset(RpgPlayer rpgPlayer) {
        PlayerData.Job job = rpgPlayer.data().job();
        job.base(null);
        job.tier1(null);
        job.tier2(null);
        saves.markDirty(rpgPlayer.data(), SavePriority.IMMEDIATE);
        stats.refresh(rpgPlayer);
    }

    /** 표시용 이름. 직업이 없으면 null. */
    public String displayName(PlayerData data) {
        JobDefinition base = tree().base(data.job().base());
        return base == null ? null : base.display();
    }
}
