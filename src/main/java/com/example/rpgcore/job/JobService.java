package com.example.rpgcore.job;

import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.config.schema.JobSettings;
import com.example.rpgcore.core.Lifecycle;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.player.data.PlayerData;
import com.example.rpgcore.stat.StatService;
import com.example.rpgcore.storage.dirty.SavePriority;
import com.example.rpgcore.storage.dirty.SaveScheduler;
import java.util.Collection;
import java.util.List;

/**
 * 지시서 3장 [job/JobService] — 직업 선택·전직 처리.
 *
 * <p>기본 직업 선택(3단계), 1차 전직(8단계), 2차 전직(9단계)을 처리한다.
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
        ALREADY_SELECTED,
        /** 기본 직업을 아직 안 골랐음 */
        NO_BASE_JOB,
        /** 1차 전직을 아직 안 했음 */
        NO_TIER1,
        /** 전직 퀘스트를 아직 안 깼음 */
        QUEST_NOT_DONE,
        /** 그런 분기가 없음 */
        UNKNOWN_BRANCH
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

    // ------------------------------------------------------------
    // 1차 전직 (8단계)
    // ------------------------------------------------------------

    /**
     * 1차 전직을 할 수 있는지.
     *
     * <p>기획서 5장 [시점]: 20레벨 + 전직 퀘스트 클리어.
     */
    public Result canAdvanceTier1(PlayerData data) {
        JobSettings settings = config.jobs();
        if (!data.job().hasBase()) {
            return Result.NO_BASE_JOB;
        }
        if (data.job().tier1() != null && !settings.branchRevert()) {
            return Result.ALREADY_SELECTED;
        }
        if (data.combat().level() < settings.tier1Level()) {
            return Result.LEVEL_TOO_LOW;
        }
        String quest = settings.tier1Quest();
        if (quest != null && !quest.isEmpty() && !data.quest().completed().contains(quest)) {
            return Result.QUEST_NOT_DONE;
        }
        return Result.OK;
    }

    /** 지금 고를 수 있는 1차 분기. 기본 직업이 없으면 빈 목록. */
    public Collection<JobBranch> tier1Choices(PlayerData data) {
        JobDefinition base = tree().base(data.job().base());
        return base == null ? List.of() : base.tier1().values();
    }

    /** 1차 전직을 한다. */
    public Result advanceTier1(RpgPlayer rpgPlayer, String branchId) {
        PlayerData data = rpgPlayer.data();
        Result check = canAdvanceTier1(data);
        if (check != Result.OK) {
            return check;
        }
        if (tree().tier1(data.job().base(), branchId) == null) {
            return Result.UNKNOWN_BRANCH;
        }
        data.job().tier1(branchId);
        saves.markDirty(data, SavePriority.IMMEDIATE);
        stats.refresh(rpgPlayer);
        return Result.OK;
    }

    /** 관리자용 1차 분기 강제 지정. 레벨과 퀘스트를 무시한다. */
    public Result forceSetTier1(RpgPlayer rpgPlayer, String branchId) {
        PlayerData data = rpgPlayer.data();
        if (!data.job().hasBase()) {
            return Result.NO_BASE_JOB;
        }
        if (tree().tier1(data.job().base(), branchId) == null) {
            return Result.UNKNOWN_BRANCH;
        }
        data.job().tier1(branchId);
        saves.markDirty(data, SavePriority.IMMEDIATE);
        stats.refresh(rpgPlayer);
        return Result.OK;
    }

    /** 지금 1차 분기. 없으면 null. */
    public JobBranch tier1Of(PlayerData data) {
        return tree().tier1(data.job().base(), data.job().tier1());
    }

    // ------------------------------------------------------------
    // 2차 전직 (9단계)
    // ------------------------------------------------------------

    /**
     * 2차 전직을 할 수 있는지.
     *
     * <p>기획서 5장 [시점]: 50레벨 + 전직 퀘스트 클리어.
     */
    public Result canAdvanceTier2(PlayerData data) {
        JobSettings settings = config.jobs();
        if (!data.job().hasBase()) {
            return Result.NO_BASE_JOB;
        }
        if (data.job().tier1() == null) {
            return Result.NO_TIER1;
        }
        if (data.job().tier2() != null && !settings.branchRevert()) {
            return Result.ALREADY_SELECTED;
        }
        if (data.combat().level() < settings.tier2Level()) {
            return Result.LEVEL_TOO_LOW;
        }
        String quest = settings.tier2Quest();
        if (quest != null && !quest.isEmpty() && !data.quest().completed().contains(quest)) {
            return Result.QUEST_NOT_DONE;
        }
        return Result.OK;
    }

    /** 지금 고를 수 있는 2차 분기. 1차 전직 전이면 빈 목록. */
    public Collection<JobBranch> tier2Choices(PlayerData data) {
        JobBranch tier1 = tier1Of(data);
        return tier1 == null ? List.of() : tier1.children().values();
    }

    /** 2차 전직을 한다. */
    public Result advanceTier2(RpgPlayer rpgPlayer, String branchId) {
        PlayerData data = rpgPlayer.data();
        Result check = canAdvanceTier2(data);
        if (check != Result.OK) {
            return check;
        }
        if (tree().tier2(data.job().base(), data.job().tier1(), branchId) == null) {
            return Result.UNKNOWN_BRANCH;
        }
        data.job().tier2(branchId);
        saves.markDirty(data, SavePriority.IMMEDIATE);
        stats.refresh(rpgPlayer);
        return Result.OK;
    }

    /** 관리자용 2차 분기 강제 지정. 레벨과 퀘스트를 무시한다. */
    public Result forceSetTier2(RpgPlayer rpgPlayer, String branchId) {
        PlayerData data = rpgPlayer.data();
        if (data.job().tier1() == null) {
            return Result.NO_TIER1;
        }
        if (tree().tier2(data.job().base(), data.job().tier1(), branchId) == null) {
            return Result.UNKNOWN_BRANCH;
        }
        data.job().tier2(branchId);
        saves.markDirty(data, SavePriority.IMMEDIATE);
        stats.refresh(rpgPlayer);
        return Result.OK;
    }

    /** 지금 2차 분기. 없으면 null. */
    public JobBranch tier2Of(PlayerData data) {
        return tree().tier2(data.job().base(), data.job().tier1(), data.job().tier2());
    }

    /** 표시용 이름. 직업이 없으면 null. */
    public String displayName(PlayerData data) {
        JobDefinition base = tree().base(data.job().base());
        if (base == null) {
            return null;
        }
        StringBuilder name = new StringBuilder(base.display());
        JobBranch tier1 = tier1Of(data);
        if (tier1 != null) {
            name.append(" / ").append(tier1.display());
        }
        JobBranch tier2 = tier2Of(data);
        if (tier2 != null) {
            name.append(" / ").append(tier2.display());
        }
        return name.toString();
    }
}
