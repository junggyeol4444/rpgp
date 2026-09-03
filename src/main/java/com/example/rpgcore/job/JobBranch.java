package com.example.rpgcore.job;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 전직 분기 하나. 1차 전직과 2차 전직에 같은 모양을 쓴다.
 *
 * <p>기획서 5장: 기본 직업당 1차 2갈래, 1차당 2차 2갈래.
 *
 * <p>3단계에서는 읽어만 두고 쓰지 않는다. 전직 처리는 8·9단계다.
 * (지시서 15장: 다음 단계 기능을 미리 절반만 만들어두지 않는다)
 *
 * @param id       분기 id
 * @param display  표시 이름
 * @param children 하위 분기. 2차 분기면 비어 있다
 */
public record JobBranch(String id, String display, Map<String, JobBranch> children) {

    public JobBranch {
        children = new LinkedHashMap<>(children);
    }

    /** 없으면 null. */
    public JobBranch child(String id) {
        return children.get(id);
    }
}
