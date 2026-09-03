package com.example.rpgcore;

import com.example.rpgcore.combat.DamagePipeline;
import com.example.rpgcore.combat.HealthService;
import com.example.rpgcore.binding.BindingService;
import com.example.rpgcore.binding.SkillItems;
import com.example.rpgcore.binding.listener.InputListener;
import com.example.rpgcore.combat.listener.CombatListener;
import com.example.rpgcore.command.RpgCommand;
import com.example.rpgcore.command.admin.AdminCommand;
import com.example.rpgcore.command.admin.DataDumpCommand;
import com.example.rpgcore.command.admin.DataResetCommand;
import com.example.rpgcore.command.admin.DebugCommand;
import com.example.rpgcore.command.admin.ExpCommand;
import com.example.rpgcore.command.admin.JobResetCommand;
import com.example.rpgcore.command.admin.ReloadCommand;
import com.example.rpgcore.command.admin.SaveCommand;
import com.example.rpgcore.command.admin.BindResetCommand;
import com.example.rpgcore.command.admin.QuestAdminCommand;
import com.example.rpgcore.command.admin.QuestCycleCommand;
import com.example.rpgcore.command.admin.QuestResetCommand;
import com.example.rpgcore.command.admin.SetJobCommand;
import com.example.rpgcore.command.admin.SetLevelCommand;
import com.example.rpgcore.command.admin.StatPointCommand;
import com.example.rpgcore.command.admin.StatResetCommand;
import com.example.rpgcore.command.admin.SkillAdminCommand;
import com.example.rpgcore.command.admin.SkillPointCommand;
import com.example.rpgcore.command.admin.StatusCommand;
import com.example.rpgcore.command.sub.InfoCommand;
import com.example.rpgcore.command.sub.BindCommand;
import com.example.rpgcore.command.sub.JobCommand;
import com.example.rpgcore.command.sub.QuestCommand;
import com.example.rpgcore.command.sub.SkillCommand;
import com.example.rpgcore.command.sub.StatCommand;
import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.config.validation.ValidationReport;
import com.example.rpgcore.core.BukkitMainThreadExecutor;
import com.example.rpgcore.core.MainThreadExecutor;
import com.example.rpgcore.core.ServiceRegistry;
import com.example.rpgcore.job.JobService;
import com.example.rpgcore.level.CombatLevelService;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.npc.NoNpcBridge;
import com.example.rpgcore.npc.NpcBridge;
import com.example.rpgcore.quest.QuestService;
import com.example.rpgcore.quest.objective.ObjectiveListener;
import com.example.rpgcore.quest.objective.ObjectiveTracker;
import com.example.rpgcore.quest.reward.RewardService;
import com.example.rpgcore.region.RegionService;
import com.example.rpgcore.skill.SkillService;
import com.example.rpgcore.skill.cooldown.CooldownService;
import com.example.rpgcore.skill.effect.DamageAreaExecutor;
import com.example.rpgcore.skill.effect.DamageConeExecutor;
import com.example.rpgcore.skill.effect.DamageTargetExecutor;
import com.example.rpgcore.skill.effect.EffectRegistry;
import com.example.rpgcore.skill.effect.HealSelfExecutor;
import com.example.rpgcore.skill.mana.ManaService;
import com.example.rpgcore.stat.StatService;
import com.example.rpgcore.storage.PlayerDataRepository;
import com.example.rpgcore.storage.cache.PlayerDataCache;
import com.example.rpgcore.storage.dirty.DirtyTracker;
import com.example.rpgcore.storage.dirty.SaveScheduler;
import com.example.rpgcore.storage.yaml.YamlRepository;
import com.example.rpgcore.ui.HudService;
import com.example.rpgcore.ui.actionbar.ActionBarChannel;
import com.example.rpgcore.ui.bossbar.BossBarChannel;
import com.example.rpgcore.ui.gui.GuiManager;
import com.example.rpgcore.ui.scoreboard.ScoreboardChannel;
import com.example.rpgcore.ui.tab.TabChannel;
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
 * <p>서비스를 만들어 {@link ServiceRegistry} 에 등록하고 순서대로 켠다.
 * 게임 로직은 각 서비스에 둔다.
 *
 * <p>현재 범위는 지시서 14장의 1단계(골격과 전투 레벨),
 * 2단계(스탯과 전투), 3단계(기본 직업), 4단계(스킬 코어),
 * 5단계(퀘스트)다. 전직과 경제·생활 트랙은 아직 없다.
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
        StatService stats = registry.register(StatService.class,
                new StatService(config, saves, players));
        JobService jobs = registry.register(JobService.class,
                new JobService(config, saves, stats));
        HealthService health = registry.register(HealthService.class, new HealthService());
        DamagePipeline pipeline = registry.register(DamagePipeline.class,
                new DamagePipeline(config, getLogger()));
        CooldownService cooldowns = registry.register(CooldownService.class,
                new CooldownService());
        ManaService mana = registry.register(ManaService.class,
                new ManaService(config, players, mainThread));
        EffectRegistry effects = new EffectRegistry()
                .register(new DamageTargetExecutor())
                .register(new DamageConeExecutor())
                .register(new DamageAreaExecutor())
                .register(new HealSelfExecutor());
        SkillService skills = registry.register(SkillService.class,
                new SkillService(config, saves, stats, mana, cooldowns, pipeline,
                        health, players, effects));
        SkillItems skillItems = new SkillItems(this, messages);
        BindingService bindings = registry.register(BindingService.class,
                new BindingService(config, saves, skills, skillItems));

        RegionService regions = registry.register(RegionService.class,
                new RegionService(config));
        RewardService rewards = new RewardService(levels, saves);
        QuestService quests = registry.register(QuestService.class,
                new QuestService(config, saves, rewards, messages));
        ObjectiveTracker objectives = new ObjectiveTracker(quests);
        // 지시서 16장 6번이 확인될 때까지 NPC 연동은 비어 있는 구현을 쓴다.
        NpcBridge npcs = new NoNpcBridge();

        GuiManager guis = registry.register(GuiManager.class, new GuiManager(players));
        HudService hud = registry.register(HudService.class,
                new HudService(players, mainThread, getLogger(),
                        config.ui().updateIntervalTicks(), config.ui().channels()));

        hud.register(new ActionBarChannel(messages, health))
                .register(new ScoreboardChannel(messages, levels))
                .register(new TabChannel(messages))
                .register(new BossBarChannel());

        // 파생 수치가 바뀌면 내부 HP 상한도 따라 움직여야 한다. (지시서 9장)
        stats.onRecalculated(health::onStatsChanged);
        // 직업 보정이 레벨에 비례하므로 레벨이 오르면 다시 계산한다. (3단계)
        levels.onLevelChanged(stats::refresh);

        players.onAttach(rpgPlayer -> {
            stats.refresh(rpgPlayer);
            health.initialize(rpgPlayer);
            hud.attach(rpgPlayer);
            // 접속할 때 일일·주간 주기가 지났는지 본다.
            quests.applyCycles(rpgPlayer, System.currentTimeMillis());
        });
        players.onDetach(rpgPlayer -> {
            hud.detach(rpgPlayer);
            guis.forget(rpgPlayer);
            rpgPlayer.inputState().clear();
            cooldowns.clear(rpgPlayer);
        });

        registry.enableAll();

        getServer().getPluginManager().registerEvents(players, this);
        getServer().getPluginManager().registerEvents(guis, this);
        getServer().getPluginManager().registerEvents(
                new CombatListener(config, players, pipeline, health), this);
        getServer().getPluginManager().registerEvents(
                new InputListener(players, bindings, skills, skillItems, messages), this);
        getServer().getPluginManager().registerEvents(
                new ObjectiveListener(players, objectives, regions, npcs), this);

        if (!registerCommands(config, repository, saves, levels, players, stats, jobs,
                skills, bindings, quests, guis, messages)) {
            getLogger().severe("명령어를 등록하지 못했습니다. plugin.yml 의 commands 항목을 확인하세요.");
        }

        // 서버가 돌아가는 중에 켜졌다면 이미 접속해 있는 플레이어가 있다.
        players.loadOnlinePlayers();

        getLogger().info(PluginIds.PLUGIN_NAME + " " + version() + " 활성화 (1~5단계)");
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
                                     StatService stats,
                                     JobService jobs,
                                     SkillService skills,
                                     BindingService bindings,
                                     QuestService quests,
                                     GuiManager guis,
                                     Messages messages) {
        AdminCommand admin = new AdminCommand(messages);
        admin.register(new ReloadCommand(registry, config, saves, messages))
                .register(new SaveCommand(saves, messages))
                .register(new StatusCommand(version(), config, repository, players, messages))
                .register(new DebugCommand(config, messages))
                .register(new SetLevelCommand(players, levels, messages))
                .register(new ExpCommand(players, levels, messages))
                .register(new StatPointCommand(players, stats, messages))
                .register(new StatResetCommand(players, stats, messages))
                .register(new SetJobCommand(players, jobs, messages))
                .register(new JobResetCommand(players, jobs, messages))
                .register(new SkillPointCommand(players, skills, messages))
                .register(new SkillAdminCommand(players, skills, messages))
                .register(new BindResetCommand(players, bindings, messages))
                .register(new QuestAdminCommand(players, quests, messages))
                .register(new QuestResetCommand(players, quests, messages))
                .register(new QuestCycleCommand(players, quests, messages))
                .register(new DataDumpCommand(players, messages))
                .register(new DataResetCommand(players, messages));

        RpgCommand root = new RpgCommand(messages);
        root.register(new InfoCommand(players, levels, jobs, messages));
        root.register(new StatCommand(config, players, stats, guis, messages));
        root.register(new JobCommand(config, players, jobs, guis, messages));
        root.register(new SkillCommand(config, players, skills, guis, messages));
        root.register(new BindCommand(config, players, bindings, skills, guis, messages));
        root.register(new QuestCommand(config, players, quests, guis, messages));
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
