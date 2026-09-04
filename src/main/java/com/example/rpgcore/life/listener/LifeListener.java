package com.example.rpgcore.life.listener;

import com.example.rpgcore.life.LifeSource;
import com.example.rpgcore.life.LifeTrackService;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;

/**
 * 지시서 11장 [생활 트랙] — 획득원 이벤트를 트랙 서비스로 넘긴다.
 *
 * <p>여기서는 "무엇을 했는지"만 뽑아 넘기고, 어느 트랙이 오르는지는
 * life.yml 을 보는 {@link LifeTrackService} 가 정한다.
 *
 * <p>지시서 11장 [주의]: 리스너에서 무거운 연산을 하지 않는다.
 */
public final class LifeListener implements Listener {

    /** 양조대의 결과 칸. 0~2번이 물약 자리다. */
    private static final int BREW_RESULT_SLOTS = 3;

    private final PlayerManager players;
    private final LifeTrackService tracks;

    public LifeListener(PlayerManager players, LifeTrackService tracks) {
        this.players = players;
        this.tracks = tracks;
    }

    /** 벌목 · 농사 · 채광. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        RpgPlayer rpgPlayer = players.get(event.getPlayer());
        if (rpgPlayer != null) {
            // TODO 플레이어가 직접 놓은 블록을 되캐는 경우를 걸러내려면
            //      설치 이력을 따로 들고 있어야 한다. 기획서에 언급이 없어
            //      지금은 걸러내지 않는다.
            tracks.grant(rpgPlayer, LifeSource.BLOCK_BREAK,
                    event.getBlock().getType().name(), 1);
        }
    }

    /** 낚시. 물고기를 실제로 건졌을 때만 센다. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        RpgPlayer rpgPlayer = players.get(event.getPlayer());
        if (rpgPlayer != null) {
            tracks.grant(rpgPlayer, LifeSource.FISHING, null, 1);
        }
    }

    /** 조리. 화로에서 결과물을 꺼낼 때 센다. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        RpgPlayer rpgPlayer = players.get(event.getPlayer());
        if (rpgPlayer != null) {
            tracks.grant(rpgPlayer, LifeSource.COOKING,
                    event.getItemType().name(), Math.max(1, event.getItemAmount()));
        }
    }

    /**
     * 제작.
     *
     * <p>시프트 클릭으로 한 번에 여러 개를 만들어도 한 번으로 센다.
     * 정확한 개수는 인벤토리 여유 칸까지 봐야 나오는데, 그 계산을
     * 클릭 이벤트 안에서 하는 것은 지시서 11장 [주의]에 어긋난다.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        RpgPlayer rpgPlayer = players.get(player);
        ItemStack result = event.getCurrentItem();
        if (rpgPlayer != null && result != null) {
            tracks.grant(rpgPlayer, LifeSource.CRAFT, result.getType().name(), 1);
        }
    }

    /**
     * 양조.
     *
     * <p>{@code BrewEvent} 는 BlockEvent 라 누가 만들었는지 알 수 없다.
     * (Paper 26.1.2 API 로 확인) 그래서 양조대에서 결과물을 꺼내는
     * 시점에 꺼낸 사람에게 준다.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrewTake(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof BrewerInventory)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= BREW_RESULT_SLOTS) {
            return;
        }
        ItemStack taken = event.getCurrentItem();
        if (taken == null) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        RpgPlayer rpgPlayer = players.get(player);
        if (rpgPlayer != null) {
            tracks.grant(rpgPlayer, LifeSource.BREW, taken.getType().name(),
                    Math.max(1, taken.getAmount()));
        }
    }
}
