package com.example.rpgcore.job;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 지시서 3장 [job/JobDefinition] — 설정에서 읽은 기본 직업 정의.
 *
 * @param id                 직업 id. 저장 파일의 job.base 값과 같다
 * @param display            표시 이름
 * @param role               역할 식별자. 기획서 TBD 1번(직업 이름)이
 *                           정해지기 전까지 코드가 참고하는 값이다
 * @param statBonusPerLevel  능력치 id -> 레벨당 보정치
 * @param tier1              1차 전직 분기 (8단계에서 쓴다)
 * @param order              jobs.yml 에 적힌 순서. GUI 배치에 쓴다
 */
public record JobDefinition(String id,
                            String display,
                            String role,
                            Map<String, Integer> statBonusPerLevel,
                            Map<String, JobBranch> tier1,
                            int order) {

    public JobDefinition {
        statBonusPerLevel = Map.copyOf(statBonusPerLevel);
        tier1 = new LinkedHashMap<>(tier1);
    }

    /** 레벨당 보정치. 없으면 0. */
    public int statBonusPerLevel(String statId) {
        Integer value = statBonusPerLevel.get(statId);
        return value == null ? 0 : value;
    }
}
