package com.example.rpgcore.quest.objective;

import com.example.rpgcore.npc.NpcBridge;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.region.RegionDefinition;
import com.example.rpgcore.region.RegionService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * 지시서 11장 — 이벤트를 목표 판정 계층으로 넘긴다.
 *
 * <p>여기서는 "무엇이 일어났는지"만 뽑아 넘기고, 퀘스트 판단은 하지 않는다.
 *
 * <p>지시서 11장 [주의]: 리스너에서 무거운 연산을 하지 않는다.
 * 특히 이동 이벤트는 호출 빈도가 매우 높아, 진행 중인 퀘스트가 없거나
 * 블록이 바뀌지 않았으면 곧바로 빠져나간다.
 */
public final class ObjectiveListener implements Listener {

    private final PlayerManager players;
    private final ObjectiveTracker tracker;
    private final RegionService regions;
    private final NpcBridge npcs;

    public ObjectiveListener(PlayerManager players, ObjectiveTracker tracker,
                             RegionService regions, NpcBridge npcs) {
        this.players = players;
        this.tracker = tracker;
        this.regions = regions;
        this.npcs = npcs;
    }

    /** 처치. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        RpgPlayer rpgPlayer = players.get(killer);
        if (!tracker.hasActive(rpgPlayer)) {
            return;
        }
        tracker.report(rpgPlayer, ObjectiveType.KILL, event.getEntity().getType().name(), 1);
    }

    /** 수집. 주운 만큼 올린다. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        RpgPlayer rpgPlayer = players.get(player);
        if (!tracker.hasActive(rpgPlayer)) {
            return;
        }
        tracker.report(rpgPlayer, ObjectiveType.COLLECT,
                event.getItem().getItemStack().getType().name(),
                event.getItem().getItemStack().getAmount());
    }

    /** 이동. 지역에 들어서면 올린다. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null
                || (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ())) {
            // 같은 블록 안에서 시선만 돌린 경우. 매 틱 들어오므로 먼저 거른다.
            return;
        }
        RpgPlayer rpgPlayer = players.get(event.getPlayer());
        if (!tracker.hasActive(rpgPlayer)) {
            return;
        }
        RegionDefinition region = regions.regionAt(to);
        if (region != null) {
            tracker.report(rpgPlayer, ObjectiveType.REACH, region.id(), 1);
        }
    }

    /**
     * 대화.
     *
     * <p>NPC 연동이 없으면 아무 개체도 NPC 로 보지 않으므로 이 목표는
     * 진행되지 않는다. (지시서 16장 6번 확인 전까지)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !npcs.available()) {
            return;
        }
        RpgPlayer rpgPlayer = players.get(event.getPlayer());
        if (!tracker.hasActive(rpgPlayer)) {
            return;
        }
        String npcId = npcs.npcIdOf(event.getRightClicked());
        if (npcId != null) {
            tracker.report(rpgPlayer, ObjectiveType.TALK, npcId, 1);
        }
    }
}
