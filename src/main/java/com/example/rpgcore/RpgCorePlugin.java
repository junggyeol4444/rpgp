package com.example.rpgcore;

import com.example.rpgcore.command.RpgCommand;
import com.example.rpgcore.command.admin.AdminCommand;
import com.example.rpgcore.command.admin.DataDumpCommand;
import com.example.rpgcore.command.admin.DataResetCommand;
import com.example.rpgcore.command.admin.DebugCommand;
import com.example.rpgcore.command.admin.ExpCommand;
import com.example.rpgcore.command.admin.ReloadCommand;
import com.example.rpgcore.command.admin.SaveCommand;
import com.example.rpgcore.command.admin.SetLevelCommand;
import com.example.rpgcore.command.admin.StatusCommand;
import com.example.rpgcore.command.sub.InfoCommand;
import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.config.validation.ValidationReport;
import com.example.rpgcore.core.BukkitMainThreadExecutor;
import com.example.rpgcore.core.MainThreadExecutor;
import com.example.rpgcore.core.ServiceRegistry;
import com.example.rpgcore.level.CombatLevelService;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.storage.PlayerDataRepository;
import com.example.rpgcore.storage.cache.PlayerDataCache;
import com.example.rpgcore.storage.dirty.DirtyTracker;
import com.example.rpgcore.storage.dirty.SaveScheduler;
import com.example.rpgcore.storage.yaml.YamlRepository;
import com.example.rpgcore.util.Messages;
import com.example.rpgcore.util.PluginIds;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 지시서 3장 — 진입점. 부트스트랩만 담당한다.
 *
 * <p>여기서는 서비스를 만들어 {@link ServiceRegistry} 에 등록하고
 * 순서대로 켜는 일만 한다. 게임 로직은 각 서비스에 둔다.
 *
 * <p>이번 단계는 1단계(골격과 전투 레벨)다. 지시서 14장에 따라
 * 스탯·직업·스킬·퀘스트는 아직 붙이지 않는다.
 */
public final class RpgCorePlugin extends JavaPlugin {

    private ServiceRegistry registry;

    @Override
    public void onEnable() {
        Messages messages = new Messages(getLogger());

        // 설정은 다른 서비스보다 먼저 읽어야 한다. 부팅 시점이라
        // 이 한 번만 메인 스레드에서 읽는다. 이후 리로드는 IO 스레드에서 한다.
        ConfigManager config = new ConfigManager(this, messages);
        ValidationReport bootReport = new ValidationReport();
        config.loadAll(bootReport);
        config.logReport(bootReport);

        registry = new ServiceRegistry();
        registry.register(ConfigManager.class, config);

        MainThreadExecutor mainThread = new BukkitMainThreadExecutor(this);
        PlayerDataRepository repository =
                registry.register(PlayerDataRepository.class,
                        new YamlRepository(getDataFolder(), getLogger()));
        PlayerDataCache cache = registry.register(PlayerDataCache.class, new PlayerDataCache());
        DirtyTracker tracker = registry.register(DirtyTracker.class, new DirtyTracker());

        SaveScheduler saves = new SaveScheduler(repository, cache, tracker, mainThread, getLogger());
        saves.applySettings(config.general().storage());
        registry.register(SaveScheduler.class, saves);

        CombatLevelService levels = registry.register(CombatLevelService.class,
                new CombatLevelService(config, saves, messages));
        PlayerManager players = registry.register(PlayerManager.class,
                new PlayerManager(cache, saves, messages, getLogger()));

        registry.enableAll();

        getServer().getPluginManager().registerEvents(players, this);
        if (!registerCommands(config, repository, saves, levels, players, messages)) {
            getLogger().severe("명령어를 등록하지 못했습니다. plugin.yml 의 commands 항목을 확인하세요.");
        }

        // 서버가 돌아가는 중에 켜졌다면 이미 접속해 있는 플레이어가 있다.
        players.loadOnlinePlayers();

        getLogger().info(PluginIds.PLUGIN_NAME + " " + version() + " 활성화 (1단계: 골격과 전투 레벨)");
    }

    @Override
    public void onDisable() {
        if (registry == null) {
            return;
        }
        List<Throwable> failures = registry.disableAll();
        for (Throwable failure : failures) {
            getLogger().log(Level.SEVERE, "서비스를 내리는 중 오류가 발생했습니다.", failure);
        }
        registry = null;
    }

    private boolean registerCommands(ConfigManager config,
                                     PlayerDataRepository repository,
                                     SaveScheduler saves,
                                     CombatLevelService levels,
                                     PlayerManager players,
                                     Messages messages) {
        AdminCommand admin = new AdminCommand(messages);
        admin.register(new ReloadCommand(registry, config, saves, messages))
                .register(new SaveCommand(saves, messages))
                .register(new StatusCommand(version(), config, repository, players, messages))
                .register(new DebugCommand(config, messages))
                .register(new SetLevelCommand(players, levels, messages))
                .register(new ExpCommand(players, levels, messages))
                .register(new DataDumpCommand(players, messages))
                .register(new DataResetCommand(players, messages));

        RpgCommand root = new RpgCommand(messages);
        root.register(new InfoCommand(players, levels, messages));
        root.register(admin);

        PluginCommand command = getCommand(PluginIds.ROOT_COMMAND);
        if (command == null) {
            return false;
        }
        command.setExecutor(root);
        command.setTabCompleter(root);
        return true;
    }

    /**
     * 버전 문자열. plugin.yml 에 build.gradle 이 주입한 값을 그대로 읽는다.
     *
     * <p>버전을 코드와 plugin.yml 두 곳에 적지 않기 위한 것이다.
     */
    private String version() {
        try (InputStream stream = getResource("plugin.yml")) {
            if (stream == null) {
                return "unknown";
            }
            YamlConfiguration description = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            return description.getString("version", "unknown");
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "plugin.yml 에서 버전을 읽지 못했습니다.", e);
            return "unknown";
        }
    }
}
