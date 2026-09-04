package com.example.rpgcore.skill;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 지시서 3장 [skill/SkillTree] — 분기 트리.
 *
 * <p>기획서 6장 [스킬트리]: 해금은 분기 트리를 따라가고, 해금한 스킬의
 * 레벨은 자유 포인트로 올린다. 분기 선택은 되돌릴 수 없다.
 *
 * <p>설정에서 만들어지는 읽기 전용 구조다. 리로드하면 통째로 갈아끼운다.
 */
public final class SkillTree {

    private final Map<String, SkillDefinition> skills;
    private final Map<String, List<SkillDefinition>> byJob;
    private final Map<String, List<SkillDefinition>> byBranchGroup;

    public SkillTree(Map<String, SkillDefinition> skills) {
        this.skills = new LinkedHashMap<>(skills);
        this.byJob = new LinkedHashMap<>();
        this.byBranchGroup = new LinkedHashMap<>();
        for (SkillDefinition skill : this.skills.values()) {
            byJob.computeIfAbsent(skill.jobId(), key -> new ArrayList<>()).add(skill);
            if (skill.inBranchGroup()) {
                byBranchGroup.computeIfAbsent(skill.branchGroup(), key -> new ArrayList<>())
                        .add(skill);
            }
        }
    }

    public static SkillTree empty() {
        return new SkillTree(Map.of());
    }

    /** 없으면 null. */
    public SkillDefinition get(String skillId) {
        return skillId == null ? null : skills.get(skillId);
    }

    public boolean has(String skillId) {
        return get(skillId) != null;
    }

    public Collection<SkillDefinition> all() {
        return skills.values();
    }

    public int size() {
        return skills.size();
    }

    /** 직업에 속한 스킬. 설정 순서를 지킨다. */
    public List<SkillDefinition> ofJob(String jobId) {
        List<SkillDefinition> found = byJob.get(jobId);
        return found == null ? List.of() : List.copyOf(found);
    }

    /** 같은 분기 그룹의 스킬 전부. 자기 자신도 들어 있다. */
    public List<SkillDefinition> branchGroup(String groupId) {
        List<SkillDefinition> found = byBranchGroup.get(groupId);
        return found == null ? List.of() : List.copyOf(found);
    }

    /**
     * 같은 분기 그룹의 다른 스킬 id.
     * 하나를 해금하면 이 목록이 영구 잠금된다. (지시서 8장 [규칙])
     */
    public Set<String> siblingsOf(SkillDefinition skill) {
        if (!skill.inBranchGroup()) {
            return Set.of();
        }
        Set<String> siblings = new LinkedHashSet<>();
        for (SkillDefinition other : branchGroup(skill.branchGroup())) {
            if (!other.id().equals(skill.id())) {
                siblings.add(other.id());
            }
        }
        return siblings;
    }

    /** 선행 스킬로 이 스킬을 지목한 스킬들. 트리 화면에서 쓴다. */
    public List<SkillDefinition> childrenOf(String skillId) {
        List<SkillDefinition> children = new ArrayList<>();
        for (SkillDefinition skill : skills.values()) {
            if (skillId.equals(skill.parentId())) {
                children.add(skill);
            }
        }
        return children;
    }
}
