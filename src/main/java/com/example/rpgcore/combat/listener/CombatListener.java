package com.example.rpgcore.combat.listener;

import com.example.rpgcore.combat.DamagePipeline;
import com.example.rpgcore.combat.HealthService;
import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * 지시서 11장 [전투] — 전투 이벤트 훅.
 *
 * <p>지시서 9장 [원칙]: 데미지 이벤트에서 바닐라 값을 취소하고 자체 계산
 * 결과로 대체한다. 그래서 이벤트를 취소한 뒤 내부 HP 에 직접 반영한다.
 *
 * <p>[남은 확인 항목 · 구동 필요]
 * 이벤트를 취소하면 바닐라 방어구·저항 보정이 함께 사라지는 대신
 * 넉백과 피격 연출도 사라진다. 이것은 API 존재 여부가 아니라 런타임
 * 동작이라 서버를 띄워 봐야 안다. 연출이 필요하면 이 클래스에서만
 * 보완한다.
 *
 * <p>지시서 11장 [주의]: 리스너에서 무거운 연산을 하지 않는다.
 * 파생 수치는 캐시에서 읽는다.
 */
public final class CombatListener implements Listener {

    private final ConfigManager config;
    private final PlayerManager players;
    private final DamagePipeline pipeline;
    private final HealthService health;

    public CombatListener(ConfigManager config, PlayerManager players,
                          DamagePipeline pipeline, HealthService health) {
        this.config = config;
        this.players = players;
        this.pipeline = pipeline;
        this.health = health;
    }

    /**
     * 모든 피해 경로가 여기를 지난다.
     * {@link EntityDamageByEntityEvent} 도 {@link EntityDamageEvent} 의
     * 하위 타입이라 리스너 하나로 받는다.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (pipeline.isAlreadyResolved(event.getEntity().getUniqueId())) {
            // 파이프라인이 이미 계산해서 넣는 중인 피해다. 그대로 통과시킨다.
            return;
        }
        RpgPlayer victim = asRpgPlayer(event.getEntity());
        RpgPlayer attacker = null;
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            attacker = resolveAttacker(byEntity.getDamager());
        }

        if (victim == null && attacker == null) {
            // 플러그인이 관여하지 않는 몬스터끼리의 싸움. 바닐라에 맡긴다.
            return;
        }

        if (attacker != null && victim != null && !config.combat().pvpEnabled()) {
            event.setCancelled(true);
            return;
        }

        DamagePipeline.Outcome outcome = pipeline.resolve(attacker, victim, event.getDamage());

        if (victim != null) {
            // 7) 내부 HP 차감 / 8) 하트 표시 갱신 (지시서 9장)
            event.setCancelled(true);
            health.damage(victim, outcome.damage());
        } else {
            // 대상이 플레이어가 아니면 바닐라 체력을 그대로 쓴다.
            // TODO 커스텀 몬스터 단계: 몬스터도 내부 HP 로 옮긴다.
            event.setDamage(outcome.damage());
        }
    }

    /** 바닐라 자연 회복이 내부 HP 를 건너뛰지 않도록 이쪽으로 돌린다. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent event) {
        RpgPlayer target = asRpgPlayer(event.getEntity());
        if (target == null) {
            return;
        }
        event.setCancelled(true);
        health.heal(target, event.getAmount());
    }

    /** 부활 시 내부 HP 를 최대치로 되돌린다. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        RpgPlayer target = players.get(event.getPlayer());
        if (target != null) {
            health.restore(target);
        }
    }

    private RpgPlayer asRpgPlayer(Entity entity) {
        return entity instanceof Player player ? players.get(player) : null;
    }

    /**
     * 때린 쪽이 플레이어인지 가려낸다.
     * 화살처럼 던진 물체는 쏜 주체를 따라간다.
     */
    private RpgPlayer resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return players.get(player);
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return players.get(player);
            }
        }
        // TODO 펫·설치물 단계: 소환한 주인을 따라가도록 넓힌다. (지시서 9장 [주의])
        return null;
    }
}
