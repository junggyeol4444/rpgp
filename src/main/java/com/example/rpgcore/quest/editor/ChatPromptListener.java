package com.example.rpgcore.quest.editor;

import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 퀘스트 에디터가 기다리는 채팅 입력을 가로챈다.
 *
 * <p>인벤토리 화면만으로는 글자를 받을 수 없어서 채팅을 쓴다.
 * 입력을 기다리는 중일 때만 가로채고, 그 외에는 손대지 않는다.
 *
 * <p>채팅 이벤트는 비동기로 온다. 여기서는 글자만 뽑아 넘기고,
 * 실제 처리는 {@link QuestEditorService} 가 메인 스레드로 옮겨서 한다.
 */
public final class ChatPromptListener implements Listener {

    private final PlayerManager players;
    private final QuestEditorService editor;

    public ChatPromptListener(PlayerManager players, QuestEditorService editor) {
        this.players = players;
        this.editor = editor;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!editor.isWaiting(event.getPlayer().getUniqueId())) {
            return;
        }
        RpgPlayer rpgPlayer = players.get(event.getPlayer());
        if (rpgPlayer == null) {
            return;
        }
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (editor.consumeChat(rpgPlayer, text)) {
            // 편집용 입력이므로 다른 사람에게 보이지 않게 한다.
            event.setCancelled(true);
        }
    }

    /** 나가면 기다리던 입력을 버린다. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        editor.clearPrompt(event.getPlayer().getUniqueId());
    }
}
