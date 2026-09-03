package com.example.rpgcore.combat;

import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.config.schema.CombatSettings;
import com.example.rpgcore.core.Lifecycle;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.stat.DerivedStat;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import org.bukkit.entity.LivingEntity;

/**
 * 지시서 9장 [전투 계산] — 커스텀 데미지 계산.
 *
 * <p>바닐라 데미지 계산 결과를 쓰지 않는다. 데미지 이벤트에서 바닐라
 * 값을 죽이고 여기서 낸 값으로 대체한다.
 *
 * <p>파이프라인 순서는 지시서 9장 그대로다.
 * 스킬·펫·설치물 데미지도 전부 여기를 지난다. 우회 경로를 만들지 않는다.
 *
 * <p>지시서 11장에 따라 여기서 무거운 연산을 하지 않는다.
 * 파생 수치는 {@code RpgPlayer} 의 캐시에서 읽는다.
 */
public final class DamagePipeline implements Lifecycle {

    private final ConfigManager config;
    private final Logger logger;

    /**
     * 이 파이프라인이 직접 피해를 넣는 중인 대상.
     *
     * <p>몬스터에게 피해를 주려면 바닐라 쪽에 값을 넘겨야 하는데, 그러면
     * 우리 리스너가 그 이벤트를 다시 계산하려 든다. 계산을 마친 대상만
     * 여기에 표시해 두고 리스너가 건너뛰게 한다.
     */
    private final Set<UUID> alreadyResolved = ConcurrentHashMap.newKeySet();

    public DamagePipeline(ConfigManager config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    @Override
    public String serviceName() {
        return "DamagePipeline";
    }

    /**
     * 피해를 계산한다.
     *
     * @param attacker       공격 주체. 플레이어가 아니면 null
     * @param victim         피격 대상. 플레이어가 아니면 null
     * @param fallbackDamage 공격 주체가 플레이어가 아닐 때 쓸 기본 위력
     *                       (커스텀 몬스터가 붙기 전까지는 바닐라 값)
     */
    public Outcome resolve(RpgPlayer attacker, RpgPlayer victim, double fallbackDamage) {
        // 1) 공격 주체 판별 / 2) 기본 위력 산출
        double power;
        if (attacker != null) {
            // TODO 아이템 체계가 생기는 단계: 무기 기여분을 여기에 더한다.
            //      지시서 9장 2번의 "무기" 항목이며, 지금은 스탯 환산값만 쓴다.
            power = attacker.derived().get(DerivedStat.PHYSICAL_DAMAGE);
        } else {
            // TODO 커스텀 몬스터 단계: mobs.yml 의 damage 를 쓴다.
            power = Math.max(0.0, fallbackDamage);
        }
        return finish(attacker, victim, power);
    }

    /**
     * 스킬 피해를 계산한다.
     *
     * <p>지시서 9장 [주의]: 스킬·펫·설치물 데미지도 전부 이 파이프라인을
     * 지나야 한다. 평타와 다른 것은 기본 위력을 어디서 가져오는지뿐이다.
     *
     * @param skillPower 스킬 위력 + 능력치 보정까지 끝난 값
     */
    public Outcome resolveSkill(RpgPlayer attacker, RpgPlayer victim, double skillPower) {
        return finish(attacker, victim, Math.max(0.0, skillPower));
    }

    /** 3번부터 6번까지는 평타와 스킬이 같다. */
    private Outcome finish(RpgPlayer attacker, RpgPlayer victim, double basePower) {
        CombatSettings settings = config.combat();
        double power = basePower;

        // 3) 바닐라 인챈트 보정
        power *= vanillaEnchantModifier(attacker);

        // 4) 치명타 판정
        boolean critical = false;
        if (attacker != null) {
            double chance = attacker.derived().get(DerivedStat.CRIT_CHANCE);
            if (chance > 0 && ThreadLocalRandom.current().nextDouble() < chance) {
                critical = true;
                double multiplier = attacker.derived().get(DerivedStat.CRIT_DAMAGE);
                power *= multiplier > 0 ? multiplier : 1.0;
            }
        }

        // 5) 대상 방어력 적용
        double defense = victim == null ? 0.0 : victim.derived().get(DerivedStat.DEFENSE);
        double afterDefense = settings.applyDefense(power, defense);

        // 6) 지역·몬스터 보정
        afterDefense *= regionModifier();

        double finalDamage = Math.max(0.0, afterDefense);
        if (config.debug()) {
            logger.info("[damage] attacker=" + name(attacker) + " victim=" + name(victim)
                    + " power=" + power + " defense=" + defense
                    + " crit=" + critical + " final=" + finalDamage);
        }
        return new Outcome(finalDamage, critical);
    }

    /**
     * 바닐라 인챈트 보정. 지시서 9장 3번.
     *
     * <p>[확인 필요 - 지시서 16장]
     * 인챈트를 읽는 API 와, 커스텀 계산에서 바닐라 인챈트 효과를 어떻게
     * 반영할지가 아직 정해지지 않았다. (기획서 5장에서 구현 단계에 정하기로 함)
     * 정해지기 전까지 보정 없이 1.0 을 돌려준다.
     */
    private double vanillaEnchantModifier(RpgPlayer attacker) {
        return 1.0;
    }

    /**
     * 지역·몬스터 보정. 지시서 9장 6번.
     *
     * <p>region / mob 패키지가 붙는 단계에서 채운다.
     */
    private double regionModifier() {
        return 1.0;
    }

    /**
     * 계산이 끝난 피해를 플레이어가 아닌 대상에게 넣는다.
     *
     * <p>몬스터는 아직 바닐라 체력을 쓰므로 바닐라 쪽에 값을 넘긴다.
     * 그 과정에서 다시 계산되지 않도록 표시를 남긴다.
     *
     * <p>TODO 커스텀 몬스터 단계: 몬스터도 내부 HP 로 옮기면 이 우회가 없어진다.
     */
    public void dealToEntity(LivingEntity target, double damage) {
        if (damage <= 0 || !Double.isFinite(damage)) {
            return;
        }
        alreadyResolved.add(target.getUniqueId());
        try {
            target.damage(damage);
        } finally {
            alreadyResolved.remove(target.getUniqueId());
        }
    }

    /** 이 파이프라인이 이미 계산해서 넣는 중인 피해인지. */
    public boolean isAlreadyResolved(UUID entityId) {
        return alreadyResolved.contains(entityId);
    }

    private static String name(RpgPlayer rpgPlayer) {
        return rpgPlayer == null ? "-" : rpgPlayer.player().getName();
    }

    /**
     * 계산 결과.
     *
     * @param damage   최종 피해량
     * @param critical 치명타였는지
     */
    public record Outcome(double damage, boolean critical) {
    }
}
