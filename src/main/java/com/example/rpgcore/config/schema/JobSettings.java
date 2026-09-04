package com.example.rpgcore.config.schema;

import com.example.rpgcore.job.JobTree;

/**
 * 지시서 8장 [jobs.yml] 의 파싱 결과.
 *
 * @param tree            직업 트리
 * @param jobSelectLevel  기본 직업을 고를 수 있는 레벨 (기획서 5장: 3)
 * @param tier1Level      1차 전직 레벨 (8단계)
 * @param tier2Level      2차 전직 레벨 (9단계)
 * @param tier1Quest      1차 전직 퀘스트 id
 * @param tier2Quest      2차 전직 퀘스트 id
 * @param branchRevert    분기 되돌리기 허용 여부. 기획서는 불가
 */
public record JobSettings(JobTree tree,
                          int jobSelectLevel,
                          int tier1Level,
                          int tier2Level,
                          String tier1Quest,
                          String tier2Quest,
                          boolean branchRevert) {

    public static JobSettings defaults() {
        return new JobSettings(JobTree.empty(), 3, 20, 50,
                "job_advance_1", "job_advance_2", false);
    }
}
