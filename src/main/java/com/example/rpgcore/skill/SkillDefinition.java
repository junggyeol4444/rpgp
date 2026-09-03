package com.example.rpgcore.skill;

import com.example.rpgcore.skill.effect.SkillEffect;
import java.util.List;
import java.util.Map;

/**
 * 지시서 3장 [skill/SkillDefinition] — 설정에서 읽은 스킬 정의.
 *
 * @param id                       스킬 id
 * @param display                  표시 이름
 * @param jobId                    소속 직업 id
 * @param stage                    해금에 필요한 직업 단계
 * @param parentId                 선행 스킬 id. 없으면 null
 * @param branchGroup              분기 그룹 id. 같은 그룹은 택일이다.
 *                                 없으면 null
 * @param manaCost                 1레벨 마나 소모
 * @param manaPerLevel             레벨당 마나 증가량
 * @param cooldownSeconds          1레벨 쿨타임 (초)
 * @param cooldownReductionPerLevel 레벨당 줄어드는 쿨타임 (초)
 * @param maxLevel                 스킬 레벨 상한. 기획서 6장 기준 9999
 * @param power                    위력 곡선
 * @param statScaling              능력치 id -> 위력에 더할 계수
 * @param effects                  실행할 효과 목록
 */
public record SkillDefinition(String id,
                              String display,
                              String jobId,
                              SkillStage stage,
                              String parentId,
                              String branchGroup,
                              double manaCost,
                              double manaPerLevel,
                              double cooldownSeconds,
                              double cooldownReductionPerLevel,
                              int maxLevel,
                              PowerScaling power,
                              Map<String, Double> statScaling,
                              List<SkillEffect> effects) {

    /** 기획서 6장 [스킬 레벨] 상한. */
    public static final int DEFAULT_MAX_LEVEL = 9999;

    public SkillDefinition {
        statScaling = Map.copyOf(statScaling);
        effects = List.copyOf(effects);
    }

    /** 지정한 레벨에서 드는 마나. */
    public double manaAt(int level) {
        double cost = manaCost + manaPerLevel * (Math.max(1, level) - 1);
        return Math.max(0, cost);
    }

    /** 지정한 레벨의 쿨타임 (초). 0 밑으로 내려가지 않는다. */
    public double cooldownAt(int level) {
        double seconds = cooldownSeconds - cooldownReductionPerLevel * (Math.max(1, level) - 1);
        return Math.max(0, seconds);
    }

    /** 지정한 레벨의 기본 위력. 능력치 보정은 포함하지 않는다. */
    public double powerAt(int level) {
        return power.powerAt(level);
    }

    /** 분기 그룹에 속해 있는지. 속해 있으면 그룹 안에서 하나만 고를 수 있다. */
    public boolean inBranchGroup() {
        return branchGroup != null;
    }

    /** 선행 스킬이 있는지. */
    public boolean hasParent() {
        return parentId != null;
    }
}
