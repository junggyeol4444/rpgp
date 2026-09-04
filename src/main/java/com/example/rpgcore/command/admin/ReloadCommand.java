package com.example.rpgcore.command.admin;

import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.config.validation.ValidationReport;
import com.example.rpgcore.core.ServiceRegistry;
import com.example.rpgcore.storage.dirty.SaveScheduler;
import com.example.rpgcore.util.CommandUtil;
import com.example.rpgcore.util.Messages;
import org.bukkit.command.CommandSender;

/**
 * /rpg admin reload — 설정 리로드.
 *
 * <p>지시서 0장 4번에 따라 파일 읽기는 IO 스레드에서 하고, 결과 반영과
 * 응답만 메인 스레드에서 처리한다.
 */
public final class ReloadCommand implements AdminSubCommand {

    private final ServiceRegistry registry;
    private final ConfigManager config;
    private final SaveScheduler saves;
    private final Messages messages;

    public ReloadCommand(ServiceRegistry registry, ConfigManager config,
                         SaveScheduler saves, Messages messages) {
        this.registry = registry;
        this.config = config;
        this.saves = saves;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String descriptionKey() {
        return "command.admin.reload.desc";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        long start = System.currentTimeMillis();
        saves.runIo(() -> {
            ValidationReport report = new ValidationReport();
            registry.reloadAll(report);
            config.logReport(report);
            long elapsed = System.currentTimeMillis() - start;
            saves.mainThread().run(() -> {
                // 주기 저장 간격 같은 값은 메인 스레드에서 반영한다.
                saves.applySettings(config.general().storage());
                CommandUtil.safeSend(messages, sender, "admin.reload.done",
                        "count", config.fileCount(),
                        "ms", elapsed);
                if (report.errorCount() > 0) {
                    CommandUtil.safeSend(messages, sender, "admin.reload.has-errors",
                            "count", report.errorCount());
                }
            });
        });
    }
}
