package com.example.rpgcore.skill.effect;

/**
 * 지시서 3장 [skill/effect] — 효과 타입별 실행기.
 *
 * <p>지시서 8장 [규칙]: 타입마다 실행기 클래스를 둔다.
 */
public interface EffectExecutor {

    /** 이 실행기가 맡는 타입. */
    EffectType type();

    /**
     * 효과를 실행한다.
     *
     * @return 실제로 무언가에 닿았으면 true
     */
    boolean execute(SkillContext context, SkillEffect effect);
}
