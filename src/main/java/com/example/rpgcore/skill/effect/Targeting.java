package com.example.rpgcore.skill.effect;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * 효과가 대상을 고르는 방법.
 *
 * <p>범위 판정은 주변 개체 목록과 벡터 계산만 쓴다. 레이 트레이스처럼
 * 버전마다 달라지기 쉬운 API 는 쓰지 않는다. (지시서 0장 5번)
 */
public final class Targeting {

    private Targeting() {
    }

    /**
     * 시전자 주위의 살아 있는 개체.
     *
     * @param range 반지름
     */
    public static List<LivingEntity> around(Player caster, double range) {
        List<LivingEntity> found = new ArrayList<>();
        for (Entity entity : caster.getNearbyEntities(range, range, range)) {
            if (entity instanceof LivingEntity living && !entity.equals(caster)) {
                found.add(living);
            }
        }
        return found;
    }

    /**
     * 시전자가 보는 방향의 부채꼴 안에 있는 개체.
     *
     * @param range        반지름
     * @param angleDegrees 부채꼴 전체 각도. 90 이면 좌우 45도씩
     */
    public static List<LivingEntity> inCone(Player caster, double range, double angleDegrees) {
        Location origin = caster.getLocation();
        Vector facing = origin.getDirection().setY(0);
        if (facing.lengthSquared() == 0) {
            return List.of();
        }
        facing.normalize();
        double halfAngle = Math.toRadians(Math.max(0, angleDegrees) / 2.0);

        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity candidate : around(caster, range)) {
            Vector toTarget = candidate.getLocation().toVector()
                    .subtract(origin.toVector()).setY(0);
            if (toTarget.lengthSquared() == 0) {
                found.add(candidate);
                continue;
            }
            double cos = facing.dot(toTarget.normalize());
            // 부동소수 오차로 acos 가 NaN 이 되지 않도록 범위를 눌러 둔다.
            double angle = Math.acos(Math.min(1.0, Math.max(-1.0, cos)));
            if (angle <= halfAngle) {
                found.add(candidate);
            }
        }
        return found;
    }

    /**
     * 시전자가 보는 방향에서 가장 가까운 개체 하나.
     * 없으면 null.
     */
    public static LivingEntity nearestInCone(Player caster, double range, double angleDegrees) {
        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        Location origin = caster.getLocation();
        for (LivingEntity candidate : inCone(caster, range, angleDegrees)) {
            double distance = candidate.getLocation().distanceSquared(origin);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }
}
