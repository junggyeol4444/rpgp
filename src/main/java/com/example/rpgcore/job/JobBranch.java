package com.example.rpgcore.job;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 전직 분기 하나. 1차 전직과 2차 전직에 같은 모양을 쓴다.
 *
 * <p>기획서 5장: 기본 직업당 1차 2갈래, 1차당 2차 2갈래.
 *
 * @param id                 분기 id
 * @param display            표시 이름
 * @param statBonusPerLevel  능력치 id -> 레벨당 보정치.
 *                           기본 직업 보정에 더해진다
 * @param children           하위 분기. 2차 분기면 비어 있다
 */
public record JobBranch(String id, String display,
                        Map<String, Integer> statBonusPerLevel,
                        Map<String, JobBranch> children) {

    public JobBranch {
        statBonusPerLevel = Map.copyOf(statBonusPerLevel);
        children = new LinkedHashMap<>(children);
    }

    /** 레벨당 보정치. 없으면 0. */
    public int statBonusPerLevel(String statId) {
        Integer value = statBonusPerLevel.get(statId);
        return value == null ? 0 : value;
    }

    /** 없으면 null. */
    public JobBranch child(String id) {
        return children.get(id);
    }
}
