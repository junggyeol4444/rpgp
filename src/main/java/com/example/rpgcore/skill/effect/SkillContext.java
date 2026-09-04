package com.example.rpgcore.skill.effect;

import com.example.rpgcore.combat.DamagePipeline;
import com.example.rpgcore.combat.HealthService;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.skill.SkillDefinition;

/**
 * 효과 실행기가 쓰는 재료 묶음.
 *
 * @param caster   시전자
 * @param skill    스킬 정의
 * @param level    시전 시점의 스킬 레벨
 * @param power    스킬 위력 + 능력치 보정까지 끝난 값
 * @param pipeline 피해 계산 통로. 우회하지 않는다 (지시서 9장 [주의])
 * @param health   내부 HP
 * @param players  대상이 플레이어인지 가려낼 때 쓴다
 */
public record SkillContext(RpgPlayer caster,
                           SkillDefinition skill,
                           int level,
                           double power,
                           DamagePipeline pipeline,
                           HealthService health,
                           PlayerManager players) {
}
