package com.example.rpgcore.quest.editor;

import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.config.validation.ValidationReport;
import com.example.rpgcore.core.Lifecycle;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.storage.dirty.SaveScheduler;
import com.example.rpgcore.util.Messages;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 지시서 14장 10단계 — 퀘스트 GUI 에디터의 상태를 들고 있는다.
 *
 * <p>편집 중인 초안과, 채팅으로 값을 받으려고 기다리는 상태를 관리한다.
 * 인벤토리 화면만으로는 글자를 받을 수 없어서 채팅을 쓴다.
 *
 * <p>파일 쓰기는 IO 스레드에서 하고, 다 쓴 뒤 메인 스레드로 돌아와
 * 퀘스트 정의를 다시 읽는다. (지시서 0장 4번)
 */
public final class QuestEditorService implements Lifecycle {

    /** 채팅 입력을 물릴 때 치는 말. */
    public static final String CANCEL_WORD = "cancel";

    /** 채팅으로 값을 하나 받으려고 기다리는 상태. */
    public record Prompt(String messageKey, Consumer<String> onInput) {
    }

    private final ConfigManager config;
    private final SaveScheduler saves;
    private final QuestWriter writer;
    private final Messages messages;
    private final Logger logger;

    private final Map<UUID, QuestDraft> drafts = new ConcurrentHashMap<>();
    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();

    public QuestEditorService(ConfigManager config, SaveScheduler saves, QuestWriter writer,
                              Messages messages, Logger logger) {
        this.config = config;
        this.saves = saves;
        this.writer = writer;
        this.messages = messages;
        this.logger = logger;
    }

    @Override
    public String serviceName() {
        return "QuestEditorService";
    }

    @Override
    public void enable() {
        // 화면이 그릴 때 파일을 열지 않도록, 관리 목록을 미리 IO 스레드에서 읽어둔다.
        saves.runIo(writer::refresh);
    }

    @Override
    public void disable() {
        drafts.clear();
        prompts.clear();
    }

    public QuestWriter writer() {
        return writer;
    }

    // ------------------------------------------------------------
    // 초안
    // ------------------------------------------------------------

    /** 없으면 null. */
    public QuestDraft draft(RpgPlayer rpgPlayer) {
        return drafts.get(rpgPlayer.uuid());
    }

    public void draft(RpgPlayer rpgPlayer, QuestDraft draft) {
        drafts.put(rpgPlayer.uuid(), draft);
    }

    public void clearDraft(RpgPlayer rpgPlayer) {
        drafts.remove(rpgPlayer.uuid());
    }

    // ------------------------------------------------------------
    // 채팅 입력
    // ------------------------------------------------------------

    /**
     * 채팅으로 값을 하나 받는다. 화면을 닫고 안내를 보낸다.
     *
     * @param messageKey 안내 문구 경로
     * @param onInput    받은 글자를 처리할 대상. 메인 스레드에서 불린다
     */
    public void prompt(RpgPlayer rpgPlayer, String messageKey, Consumer<String> onInput) {
        prompts.put(rpgPlayer.uuid(), new Prompt(messageKey, onInput));
        rpgPlayer.player().closeInventory();
        messages.send(rpgPlayer.player(), messageKey);
        messages.send(rpgPlayer.player(), "editor.prompt.cancel", "word", CANCEL_WORD);
    }

    /** 이 플레이어가 입력을 기다리는 중인지. */
    public boolean isWaiting(UUID uuid) {
        return prompts.containsKey(uuid);
    }

    /**
     * 채팅으로 들어온 글자를 처리한다.
     *
     * <p>채팅 이벤트는 다른 스레드에서 오므로, 실제 처리는 메인 스레드로
     * 넘긴 뒤에 한다.
     *
     * @return 입력을 가져갔으면 true. 그러면 채팅을 취소해야 한다
     */
    public boolean consumeChat(RpgPlayer rpgPlayer, String text) {
        Prompt prompt = prompts.remove(rpgPlayer.uuid());
        if (prompt == null) {
            return false;
        }
        saves.mainThread().run(() -> {
            if (CANCEL_WORD.equalsIgnoreCase(text.trim())) {
                messages.send(rpgPlayer.player(), "editor.prompt.cancelled");
                return;
            }
            prompt.onInput().accept(text.trim());
        });
        return true;
    }

    public void clearPrompt(UUID uuid) {
        prompts.remove(uuid);
    }

    // ------------------------------------------------------------
    // 저장
    // ------------------------------------------------------------

    /**
     * 초안을 파일에 쓰고 퀘스트 정의를 다시 읽는다.
     *
     * <p>쓰기와 다시 읽기 모두 IO 스레드에서 하고, 결과 안내만 메인
     * 스레드에서 보낸다.
     */
    public void save(RpgPlayer rpgPlayer, QuestDraft draft) {
        if (!draft.isValid()) {
            messages.send(rpgPlayer.player(), "editor.save.no-objective");
            return;
        }
        saves.runIo(() -> {
            boolean written = true;
            try {
                writer.write(draft.toDefinition());
            } catch (IOException e) {
                logger.log(Level.SEVERE, "퀘스트를 저장하지 못했습니다: " + draft.id(), e);
                written = false;
            }

            int errors = 0;
            if (written) {
                ValidationReport report = new ValidationReport();
                config.reloadQuests(report);
                config.logReport(report);
                errors = report.errorCount();
            }

            boolean saved = written;
            int errorCount = errors;
            saves.mainThread().run(() -> {
                if (!saved) {
                    messages.send(rpgPlayer.player(), "editor.save.failed");
                    return;
                }
                messages.send(rpgPlayer.player(), "editor.save.done",
                        "quest", draft.display(), "id", draft.id());
                if (errorCount > 0) {
                    // 파일은 썼지만 다시 읽는 과정에서 걸린 것이 있다.
                    messages.send(rpgPlayer.player(), "editor.save.warned",
                            "count", errorCount);
                }
            });
        });
    }

    /**
     * 에디터가 만든 퀘스트를 지우고 다시 읽는다.
     *
     * @param after 다시 읽기까지 끝난 뒤 메인 스레드에서 부를 것. 목록 화면을
     *              다시 그리는 데 쓴다. 없으면 null
     */
    public void delete(RpgPlayer rpgPlayer, String questId, Runnable after) {
        saves.runIo(() -> {
            boolean removed = true;
            try {
                writer.delete(questId);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "퀘스트를 지우지 못했습니다: " + questId, e);
                removed = false;
            }
            if (removed) {
                ValidationReport report = new ValidationReport();
                config.reloadQuests(report);
                config.logReport(report);
            }

            boolean done = removed;
            saves.mainThread().run(() -> {
                messages.send(rpgPlayer.player(),
                        done ? "editor.delete.done" : "editor.delete.failed",
                        "quest", questId);
                if (done && after != null && rpgPlayer.isOnline()) {
                    after.run();
                }
            });
        });
    }
}
