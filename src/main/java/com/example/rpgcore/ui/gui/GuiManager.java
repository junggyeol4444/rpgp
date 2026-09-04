package com.example.rpgcore.ui.gui;

import com.example.rpgcore.core.Lifecycle;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * 지시서 13장 [공통] — 열려 있는 GUI 를 플레이어별로 추적하고,
 * 퇴장·리로드 시 정리한다.
 *
 * <p>클릭 이벤트는 전부 취소한 뒤 화면에 넘긴다.
 */
public final class GuiManager implements Lifecycle, Listener {

    private final PlayerManager players;
    private final Map<UUID, Gui> open = new HashMap<>();

    public GuiManager(PlayerManager players) {
        this.players = players;
    }

    @Override
    public String serviceName() {
        return "GuiManager";
    }

    @Override
    public void disable() {
        // 열린 창을 닫아두지 않으면 아이템을 들고 있는 상태로 남을 수 있다.
        for (UUID uuid : List.copyOf(open.keySet())) {
            RpgPlayer rpgPlayer = players.get(uuid);
            if (rpgPlayer != null && rpgPlayer.isOnline()) {
                rpgPlayer.player().closeInventory();
            }
        }
        open.clear();
    }

    /** 화면을 그려서 연다. */
    public void open(RpgPlayer rpgPlayer, Gui gui) {
        gui.render(rpgPlayer);
        open.put(rpgPlayer.uuid(), gui);
        rpgPlayer.player().openInventory(gui.getInventory());
    }

    /** 지금 열려 있는 화면. 없으면 null. */
    public Gui opened(UUID uuid) {
        return open.get(uuid);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof Gui gui)) {
            return;
        }
        // 우리 화면에서는 어떤 클릭도 아이템을 움직이지 못한다.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        RpgPlayer rpgPlayer = players.get(player);
        if (rpgPlayer == null) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= gui.screen().size()) {
            // 아래쪽 자기 인벤토리를 누른 경우. 취소만 하고 넘긴다.
            return;
        }
        gui.onClick(rpgPlayer, slot, event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Gui gui)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        open.remove(player.getUniqueId());
        RpgPlayer rpgPlayer = players.get(player);
        if (rpgPlayer != null) {
            gui.onClose(rpgPlayer);
        }
    }

    /** 퇴장 시 추적 제거. PlayerManager 의 detach 에 연결한다. */
    public void forget(RpgPlayer rpgPlayer) {
        open.remove(rpgPlayer.uuid());
    }
}
