package com.example.rpgcore.skill.effect;

import java.util.List;
import org.bukkit.entity.LivingEntity;

/**
 * DAMAGE_CONE — 보는 방향의 부채꼴 범위.
 *
 * <p>설정값: range (기본 4.0), angle (기본 90)
 */
public final class DamageConeExecutor extends DamageEffectExecutor {

    @Override
    public EffectType type() {
        return EffectType.DAMAGE_CONE;
    }

    @Override
    protected List<LivingEntity> pickTargets(SkillContext context, SkillEffect effect) {
        return Targeting.inCone(context.caster().player(),
                effect.value("range", 4.0), effect.value("angle", 90.0));
    }
}
