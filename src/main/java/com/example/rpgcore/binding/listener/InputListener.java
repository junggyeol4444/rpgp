package com.example.rpgcore.binding.listener;

import com.example.rpgcore.binding.BindingService;
import com.example.rpgcore.binding.HoldState;
import com.example.rpgcore.binding.InputTrigger;
import com.example.rpgcore.binding.SkillItems;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.skill.SkillService;
import com.example.rpgcore.util.Messages;
import java.util.Locale;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * 지시서 10장 / 11장 [입력] — 키 조합과 스킬 아이템 발동.
 *
 * <p>지시서 10장 [바닐라 기능 처리]:
 * F·Q·E 처럼 원래 기능이 있는 입력은 기능을 그대로 둔다.
 * 유지 상태(웅크림·달리기)가 성립한 상태에서만 스킬 발동으로 해석하고,
 * 그때 해당 이벤트를 취소한다. 유지 상태가 없으면 아무 것도 하지 않는다.
 *
 * <p>[확인 필요 - 지시서 16장 4번]
 * 점프는 Paper 전용 이벤트 존재 여부가 확인되지 않아 빠져 있다.
 * 확인되면 여기에 핸들러를 더하고 조합 수가 12에서 14로 늘어난다.
 *
 * <p>[확인 필요 - 지시서 16장 5번]
 * 좌클릭 허공 감지는 버전마다 이벤트 동작이 다를 수 있다.
 * 실제 구동으로 확인한 뒤 확정한다.
 */
public final class InputListener implements Listener {

    private final PlayerManager players;
    private final BindingService bindings;
    private final SkillService skills;
    private final SkillItems items;
    private final Messages messages;

    public InputListener(PlayerManager players, BindingService bindings, SkillService skills,
                         SkillItems items, Messages messages) {
        this.players = players;
        this.bindings = bindings;
        this.skills = skills;
        this.items = items;
        this.messages = messages;
    }

    // ------------------------------------------------------------
    // 유지 상태 (지시서 10장)
    // ------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        RpgPlayer rpgPlayer = players.get(event.getPlayer());
        if (rpgPlayer != null) {
            rpgPlayer.inputState().sneaking(event.isSneaking());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSprint(PlayerToggleSprintEvent event) {
        RpgPlayer rpgPlayer = players.get(event.getPlayer());
        if (rpgPlayer != null) {
            rpgPlayer.inputState().sprinting(event.isSprinting());
        }
    }

    // ------------------------------------------------------------
    // 순간 입력
    // ------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            // 양손이 각각 이벤트를 내므로 주손만 본다.
            return;
        }
        RpgPlayer rpgPlayer = players.get(event.getPlayer());
        if (rpgPlayer == null) {
            return;
        }
        Action action = event.getAction();
        boolean rightClick = action == Action.RIGHT_CLICK_AIR
                || action == Action.RIGHT_CLICK_BLOCK;
        boolean leftClick = action == Action.LEFT_CLICK_AIR
                || action == Action.LEFT_CLICK_BLOCK;
        if (!rightClick && !leftClick) {
            return;
        }

        if (handle(rpgPlayer, rightClick ? InputTrigger.RIGHT_CLICK : InputTrigger.LEFT_CLICK,
                event)) {
            return;
        }

        // 조합이 안 걸렸으면 스킬 아이템 우클릭인지 본다.
        if (!rightClick) {
            return;
        }
        String skillId = items.skillIdOf(event.getItem());
        if (skillId != null) {
            event.setCancelled(true);
            cast(rpgPlayer, skillId);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        RpgPlayer rpgPlayer = players.get(event.getPlayer());
        if (rpgPlayer != null) {
            handle(rpgPlayer, InputTrigger.SWAP_HAND, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        RpgPlayer rpgPlayer = players.get(event.getPlayer());
        if (rpgPlayer == null) {
            return;
        }
        // 지시서 10장: 스킬 아이템은 버릴 수 없다.
        if (items.isSkillItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            messages.send(event.getPlayer(), "skill.item.locked");
            return;
        }
        handle(rpgPlayer, InputTrigger.DROP_ITEM, event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        RpgPlayer rpgPlayer = players.get(player);
        if (rpgPlayer != null) {
            handle(rpgPlayer, InputTrigger.OPEN_INVENTORY, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        RpgPlayer rpgPlayer = players.get(event.getPlayer());
        if (rpgPlayer != null) {
            handle(rpgPlayer, InputTrigger.SLOT_CHANGE, event);
        }
    }

    /** 지시서 10장: 스킬 아이템은 보관함에서 옮길 수 없다. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (items.isSkillItem(event.getCurrentItem())
                || items.isSkillItem(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------
    // 공통
    // ------------------------------------------------------------

    /**
     * 유지 상태가 성립하고 그 조합에 스킬이 걸려 있으면 발동시킨다.
     *
     * @return 발동시켰으면 true
     */
    private boolean handle(RpgPlayer rpgPlayer, InputTrigger trigger, Cancellable event) {
        HoldState hold = rpgPlayer.inputState().active();
        if (hold == null) {
            return false;
        }
        String skillId = bindings.comboSkill(rpgPlayer.data(), hold, trigger);
        if (skillId == null) {
            return false;
        }
        event.setCancelled(true);
        cast(rpgPlayer, skillId);
        return true;
    }

    private void cast(RpgPlayer rpgPlayer, String skillId) {
        SkillService.Result result = skills.cast(rpgPlayer, skillId);
        if (result != SkillService.Result.OK) {
            messages.send(rpgPlayer.player(), "skill.cast." + key(result));
        }
    }

    /** 열거형 이름을 messages.yml 경로로 쓸 수 있게 바꾼다. */
    private static String key(SkillService.Result result) {
        return result.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
