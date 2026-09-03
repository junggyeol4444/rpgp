package com.example.rpgcore.skill.effect;

import com.example.rpgcore.combat.DamagePipeline;
import com.example.rpgcore.player.RpgPlayer;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * 피해를 주는 효과 세 가지(단일 · 부채꼴 · 원형)의 공통 부분.
 *
 * <p>어떤 경로든 {@link DamagePipeline} 을 지난다.
 * 지시서 9장 [주의]: 우회 경로를 만들지 않는다.
 */
abstract class DamageEffectExecutor implements EffectExecutor {

    /** 이 효과가 때릴 대상. */
    protected abstract List<LivingEntity> pickTargets(SkillContext context, SkillEffect effect);

    @Override
    public boolean execute(SkillContext context, SkillEffect effect) {
        List<LivingEntity> targets = pickTargets(context, effect);
        if (targets.isEmpty()) {
            return false;
        }
        boolean hit = false;
        for (LivingEntity target : targets) {
            hit |= damage(context, target);
        }
        return hit;
    }

    private boolean damage(SkillContext context, LivingEntity target) {
        RpgPlayer victim = target instanceof Player player ? context.players().get(player) : null;
        DamagePipeline.Outcome outcome =
                context.pipeline().resolveSkill(context.caster(), victim, context.power());
        if (outcome.damage() <= 0) {
            return false;
        }
        if (victim != null) {
            context.health().damage(victim, outcome.damage());
        } else {
            context.pipeline().dealToEntity(target, outcome.damage());
        }
        return true;
    }
}
