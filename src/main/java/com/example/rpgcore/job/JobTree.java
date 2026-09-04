package com.example.rpgcore.job;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 지시서 3장 [job/JobTree] — 기본 → 1차 → 2차 트리.
 *
 * <p>설정에서 만들어지는 읽기 전용 구조다. 리로드하면 통째로 갈아끼운다.
 */
public final class JobTree {

    private final Map<String, JobDefinition> baseJobs;

    public JobTree(Map<String, JobDefinition> baseJobs) {
        this.baseJobs = new LinkedHashMap<>(baseJobs);
    }

    public static JobTree empty() {
        return new JobTree(Map.of());
    }

    /** 기본 직업 전체. jobs.yml 순서를 지킨다. */
    public Collection<JobDefinition> baseJobs() {
        return baseJobs.values();
    }

    /** 없으면 null. */
    public JobDefinition base(String id) {
        return id == null ? null : baseJobs.get(id);
    }

    public boolean hasBase(String id) {
        return base(id) != null;
    }

    public int size() {
        return baseJobs.size();
    }

    /**
     * 1차 분기를 찾는다. 없으면 null.
     * 8단계에서 쓴다.
     */
    public JobBranch tier1(String baseId, String tier1Id) {
        JobDefinition base = base(baseId);
        return base == null || tier1Id == null ? null : base.tier1().get(tier1Id);
    }

    /** 2차 분기를 찾는다. 없으면 null. */
    public JobBranch tier2(String baseId, String tier1Id, String tier2Id) {
        JobBranch tier1 = tier1(baseId, tier1Id);
        return tier1 == null || tier2Id == null ? null : tier1.child(tier2Id);
    }

    /**
     * 1차 분기를 모르는 상태에서 2차 분기를 찾는다.
     *
     * <p>스킬 정의의 requireBranch 를 확인할 때 쓴다. 그 값이 어느 1차
     * 분기 아래에 있는지까지는 설정에 적지 않기 때문이다.
     *
     * @return 없으면 null
     */
    public JobBranch findTier2(String baseId, String tier2Id) {
        JobDefinition base = base(baseId);
        if (base == null || tier2Id == null) {
            return null;
        }
        for (JobBranch tier1 : base.tier1().values()) {
            JobBranch found = tier1.child(tier2Id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
