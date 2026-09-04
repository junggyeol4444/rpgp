package com.example.rpgcore.skill.effect;

import java.util.List;
import org.bukkit.entity.LivingEntity;

/**
 * DAMAGE_AREA — 자기 주위 원형 범위.
 *
 * <p>설정값: range (기본 4.0)
 */
public final class DamageAreaExecutor extends DamageEffectExecutor {

    @Override
    public EffectType type() {
        return EffectType.DAMAGE_AREA;
    }

    @Override
    protected List<LivingEntity> pickTargets(SkillContext context, SkillEffect effect) {
        return Targeting.around(context.caster().player(), effect.value("range", 4.0));
    }
}
