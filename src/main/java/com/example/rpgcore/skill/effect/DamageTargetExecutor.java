package com.example.rpgcore.skill.effect;

import java.util.List;
import org.bukkit.entity.LivingEntity;

/**
 * DAMAGE_TARGET — 보는 방향에서 가장 가까운 대상 하나.
 *
 * <p>설정값: range (기본 4.0), angle (기본 30)
 */
public final class DamageTargetExecutor extends DamageEffectExecutor {

    @Override
    public EffectType type() {
        return EffectType.DAMAGE_TARGET;
    }

    @Override
    protected List<LivingEntity> pickTargets(SkillContext context, SkillEffect effect) {
        LivingEntity target = Targeting.nearestInCone(context.caster().player(),
                effect.value("range", 4.0), effect.value("angle", 30.0));
        return target == null ? List.of() : List.of(target);
    }
}
