package com.example.rpgcore.skill.effect;

/**
 * HEAL_SELF — 자신을 회복한다.
 *
 * <p>회복량은 스킬 위력을 그대로 쓴다.
 * 설정값 ratio 로 위력 대비 비율을 조정할 수 있다 (기본 1.0).
 *
 * <p>회복도 {@code HealthService} 를 지난다. 내부 HP 를 건드리는 지점을
 * 한 곳으로 모으기 위해서다. (지시서 9장)
 */
public final class HealSelfExecutor implements EffectExecutor {

    @Override
    public EffectType type() {
        return EffectType.HEAL_SELF;
    }

    @Override
    public boolean execute(SkillContext context, SkillEffect effect) {
        double amount = context.power() * effect.value("ratio", 1.0);
        if (amount <= 0) {
            return false;
        }
        double before = context.caster().health();
        double after = context.health().heal(context.caster(), amount);
        return after > before;
    }
}
