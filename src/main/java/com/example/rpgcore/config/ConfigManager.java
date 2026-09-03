package com.example.rpgcore.config;

import com.example.rpgcore.config.schema.CombatSettings;
import com.example.rpgcore.config.schema.CurveSettings;
import com.example.rpgcore.config.schema.GuiIcon;
import com.example.rpgcore.config.schema.JobSettings;
import com.example.rpgcore.config.schema.GuiScreen;
import com.example.rpgcore.config.schema.GeneralSettings;
import com.example.rpgcore.config.schema.LevelSettings;
import com.example.rpgcore.config.schema.ResetSettings;
import com.example.rpgcore.config.schema.StatSettings;
import com.example.rpgcore.config.schema.StorageSettings;
import com.example.rpgcore.config.schema.UiSettings;
import com.example.rpgcore.config.validation.ValidationReport;
import com.example.rpgcore.core.Reloadable;
import com.example.rpgcore.job.JobBranch;
import com.example.rpgcore.job.JobDefinition;
import com.example.rpgcore.job.JobTree;
import com.example.rpgcore.level.ExpSource;
import com.example.rpgcore.stat.DerivedStat;
import com.example.rpgcore.stat.StatType;
import com.example.rpgcore.util.Messages;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * 지시서 3장 [config/ConfigManager] — 파일 로드·검증·리로드.
 *
 * <p>지시서 6장에 따라 잘못된 항목은 그 항목만 건너뛰고 파일명·경로·이유를
 * 리포트에 남긴다. 서버를 죽이지 않는다.
 *
 * <p>이번 단계(1단계)에서 실제로 해석하는 파일은 config.yml, levels.yml,
 * messages.yml 세 개다. 나머지 파일은 기본값을 디스크에 깔아두기만 하고
 * 해당 단계에서 해석기를 붙인다. (지시서 14장: 지정된 단계 밖의 기능을
 * 미리 만들지 않는다)
 *
 * <p>[확인 필요 - 지시서 16장]
 * Bukkit 설정 API 사용처는 이 클래스와 YamlRepository 두 곳뿐이다.
 */
public final class ConfigManager implements Reloadable {

    /** 플러그인 폴더에 깔아 둘 기본 설정 파일. 지시서 6장 목록. */
    private static final List<String> DEFAULT_FILES = List.of(
            "config.yml",
            "levels.yml",
            "stats.yml",
            "jobs.yml",
            "skills.yml",
            "life.yml",
            "quests.yml",
            "regions.yml",
            "mobs.yml",
            "economy.yml",
            "gui.yml",
            "messages.yml");

    // TODO 4단계 / 5단계: 지시서 6장에 따라 skills, quests 는 디렉터리 스캔을
    //      지원해야 한다. 해당 단계에서 스캔 로직을 붙인다.

    private final Plugin plugin;
    private final Logger logger;
    private final Messages messages;

    private volatile GeneralSettings general = GeneralSettings.defaults();
    private volatile LevelSettings levels = LevelSettings.defaults();
    private volatile StatSettings stats = StatSettings.defaults();
    private volatile CombatSettings combat = CombatSettings.defaults();
    private volatile UiSettings ui = UiSettings.defaults();
    private volatile JobSettings jobs = JobSettings.defaults();
    private volatile Map<String, GuiScreen> guiScreens = new LinkedHashMap<>();
    private volatile boolean debug;
    private volatile int lastErrorCount;

    public ConfigManager(Plugin plugin, Messages messages) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.messages = messages;
    }

    @Override
    public void reload(ValidationReport report) {
        loadAll(report);
    }

    /** 파일을 전부 다시 읽는다. 디스크에 닿으므로 IO 스레드에서 부른다. */
    public synchronized void loadAll(ValidationReport report) {
        writeDefaults();
        loadGeneral(report);
        loadLevels(report);
        loadStats(report);
        loadJobs(report);
        loadGui(report);
        loadMessages(report);
        this.lastErrorCount = report.errorCount();
    }

    /** 읽어들인 설정 파일 개수. */
    public int fileCount() {
        return DEFAULT_FILES.size();
    }

    public GeneralSettings general() {
        return general;
    }

    public LevelSettings levels() {
        return levels;
    }

    public StatSettings stats() {
        return stats;
    }

    public CombatSettings combat() {
        return combat;
    }

    public UiSettings ui() {
        return ui;
    }

    public JobSettings jobs() {
        return jobs;
    }

    /** gui.yml 의 화면. 없으면 기본 크기의 빈 화면을 준다. */
    public GuiScreen guiScreen(String id) {
        GuiScreen screen = guiScreens.get(id);
        return screen == null ? GuiScreen.fallback(id) : screen;
    }

    public Messages messages() {
        return messages;
    }

    public int lastErrorCount() {
        return lastErrorCount;
    }

    /** 디버그 모드. config.yml 초기값을 /rpg admin debug 로 덮어쓸 수 있다. */
    public boolean debug() {
        return debug;
    }

    public void debug(boolean debug) {
        this.debug = debug;
    }

    /** 리포트를 콘솔에 남긴다. 파일명·경로·이유가 함께 나온다. */
    public void logReport(ValidationReport report) {
        for (ValidationReport.Entry entry : report.entries()) {
            if (entry.severity() == ValidationReport.Severity.ERROR) {
                logger.warning(entry.toString());
            } else {
                logger.info(entry.toString());
            }
        }
    }

    // ------------------------------------------------------------
    // 파일별 해석
    // ------------------------------------------------------------

    private void writeDefaults() {
        File folder = plugin.getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            logger.warning("플러그인 폴더를 만들지 못했습니다: " + folder.getAbsolutePath());
        }
        for (String name : DEFAULT_FILES) {
            File target = new File(folder, name);
            if (!target.exists()) {
                // 두 번째 인자 false: 이미 있는 파일은 덮지 않는다.
                plugin.saveResource(name, false);
            }
        }
    }

    private void loadGeneral(ValidationReport report) {
        String file = "config.yml";
        YamlConfiguration config = read(file, report);
        if (config == null) {
            general = GeneralSettings.defaults();
            debug = general.debug();
            return;
        }

        String type = config.getString("storage.type", StorageSettings.YAML);
        if (!StorageSettings.YAML.equalsIgnoreCase(type)) {
            report.error(file, "storage.type",
                    "지금 구현된 저장소는 " + StorageSettings.YAML + " 뿐입니다: " + type);
            type = StorageSettings.YAML;
        }

        int interval = config.getInt("storage.autoSaveIntervalSeconds", 300);
        if (interval < 10) {
            report.error(file, "storage.autoSaveIntervalSeconds",
                    "10초보다 짧게는 둘 수 없어 300으로 되돌립니다: " + interval);
            interval = 300;
        }

        int threads = config.getInt("storage.ioThreads", 2);
        if (threads < 1 || threads > 8) {
            report.error(file, "storage.ioThreads",
                    "1 이상 8 이하여야 해서 2로 되돌립니다: " + threads);
            threads = 2;
        }

        CombatSettings combatDefaults = CombatSettings.defaults();
        double defenseConstant =
                config.getDouble("combat.defenseConstant", combatDefaults.defenseConstant());
        if (defenseConstant < 1.0) {
            report.error(file, "combat.defenseConstant",
                    "1 이상이어야 해서 기본값으로 되돌립니다: " + defenseConstant);
            defenseConstant = combatDefaults.defenseConstant();
        }
        double minimumDamage =
                config.getDouble("combat.minimumDamage", combatDefaults.minimumDamage());
        if (minimumDamage < 0.0) {
            report.error(file, "combat.minimumDamage",
                    "음수는 둘 수 없어 0으로 되돌립니다: " + minimumDamage);
            minimumDamage = 0.0;
        }
        combat = new CombatSettings(defenseConstant, minimumDamage,
                config.getBoolean("combat.pvpEnabled", combatDefaults.pvpEnabled()));

        UiSettings uiDefaults = UiSettings.defaults();
        long uiInterval = config.getInt("ui.updateIntervalTicks",
                (int) uiDefaults.updateIntervalTicks());
        if (uiInterval < 1) {
            report.error(file, "ui.updateIntervalTicks",
                    "1 이상이어야 해서 기본값으로 되돌립니다: " + uiInterval);
            uiInterval = uiDefaults.updateIntervalTicks();
        }
        Set<String> channels = new LinkedHashSet<>();
        ConfigurationSection channelSection = config.getConfigurationSection("ui.channels");
        if (channelSection == null) {
            channels.addAll(uiDefaults.channels());
        } else {
            for (String key : channelSection.getKeys(false)) {
                if (channelSection.getBoolean(key, true)) {
                    channels.add(key);
                }
            }
        }
        ui = new UiSettings(uiInterval, channels);

        boolean debugEnabled = config.getBoolean("debug.enabled", false);
        general = new GeneralSettings(new StorageSettings(type, interval, threads), debugEnabled);
        debug = debugEnabled;
    }

    private void loadLevels(ValidationReport report) {
        String file = "levels.yml";
        YamlConfiguration config = read(file, report);
        if (config == null) {
            levels = LevelSettings.defaults();
            return;
        }

        CurveSettings combatCurve = readCurve(config, "combat.curve", CurveSettings.defaultCombat());
        CurveSettings lifeCurve = readCurve(config, "life.curve", CurveSettings.defaultLife());

        int combatMax = config.getInt("combat.maxLevel", -1);
        if (combatMax == 0 || combatMax < -1) {
            report.error(file, "combat.maxLevel",
                    "-1(상한 없음) 이거나 1 이상이어야 해서 -1로 되돌립니다: " + combatMax);
            combatMax = -1;
        }
        int lifeMax = config.getInt("life.maxLevel", -1);
        if (lifeMax == 0 || lifeMax < -1) {
            report.error(file, "life.maxLevel",
                    "-1(상한 없음) 이거나 1 이상이어야 해서 -1로 되돌립니다: " + lifeMax);
            lifeMax = -1;
        }

        int statPoints = config.getInt("combat.rewardPerLevel.statPoints", 5);
        if (statPoints < 0) {
            report.error(file, "combat.rewardPerLevel.statPoints",
                    "음수는 둘 수 없어 0으로 되돌립니다: " + statPoints);
            statPoints = 0;
        }
        int skillPoints = config.getInt("combat.rewardPerLevel.skillPoints", 1);
        if (skillPoints < 0) {
            report.error(file, "combat.rewardPerLevel.skillPoints",
                    "음수는 둘 수 없어 0으로 되돌립니다: " + skillPoints);
            skillPoints = 0;
        }

        Set<ExpSource> enabled = EnumSet.noneOf(ExpSource.class);
        for (ExpSource source : ExpSource.values()) {
            if (!source.configurable()) {
                enabled.add(source);
                continue;
            }
            if (config.getBoolean("combat.expSources." + source.configKey(), true)) {
                enabled.add(source);
            }
        }

        // 곡선 값 자체가 잘못된 경우는 ExpCurve.from 에서 다시 걸러진다.
        levels = new LevelSettings(combatCurve, combatMax, statPoints, skillPoints,
                enabled, lifeCurve, lifeMax);
    }

    private CurveSettings readCurve(ConfigurationSection config, String path,
                                    CurveSettings fallback) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return fallback;
        }
        return new CurveSettings(
                section.getString("type", fallback.type()),
                section.getDouble("base", fallback.base()),
                section.getDouble("factor", fallback.factor()));
    }

    private void loadStats(ValidationReport report) {
        String file = "stats.yml";
        YamlConfiguration config = read(file, report);
        if (config == null) {
            stats = StatSettings.defaults();
            return;
        }

        Map<String, StatType> parsed = new LinkedHashMap<>();
        ConfigurationSection statsSection = config.getConfigurationSection("stats");
        if (statsSection == null) {
            report.error(file, "stats", "능력치 정의가 없습니다. 스탯 분배를 쓸 수 없습니다.");
        } else {
            int order = 0;
            for (String id : statsSection.getKeys(false)) {
                ConfigurationSection statSection = statsSection.getConfigurationSection(id);
                if (statSection == null) {
                    report.error(file, "stats." + id, "형식이 맞지 않아 건너뜁니다.");
                    continue;
                }
                Map<DerivedStat, Double> perPoint =
                        readDerived(statSection.getConfigurationSection("derived"),
                                file, "stats." + id + ".derived", report);
                parsed.put(id, new StatType(id,
                        statSection.getString("display", id), perPoint, order++));
            }
        }

        Map<DerivedStat, Double> base = readDerived(config.getConfigurationSection("base"),
                file, "base", report);

        ResetSettings defaults = ResetSettings.defaults();
        ResetSettings reset = new ResetSettings(
                config.getBoolean("reset.allowed", defaults.allowed()),
                config.getString("reset.cost.currency", defaults.currencyId()),
                Math.max(0, (long) config.getDouble("reset.cost.amount", defaults.amount())),
                Math.max(1.0, config.getDouble("reset.scaling", defaults.scaling())));

        stats = new StatSettings(parsed, base, reset);
    }

    /** derived 블록을 읽는다. 모르는 키는 그 항목만 건너뛴다. */
    private Map<DerivedStat, Double> readDerived(ConfigurationSection section, String file,
                                                 String path, ValidationReport report) {
        Map<DerivedStat, Double> result = new EnumMap<>(DerivedStat.class);
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            DerivedStat derived = DerivedStat.fromConfigKey(key);
            if (derived == null) {
                report.error(file, path + "." + key, "알 수 없는 파생 수치라 건너뜁니다.");
                continue;
            }
            result.put(derived, section.getDouble(key, 0.0));
        }
        return result;
    }

    private void loadJobs(ValidationReport report) {
        String file = "jobs.yml";
        YamlConfiguration config = read(file, report);
        if (config == null) {
            jobs = JobSettings.defaults();
            return;
        }
        JobSettings defaults = JobSettings.defaults();

        int selectLevel = config.getInt("jobSelectLevel", defaults.jobSelectLevel());
        if (selectLevel < 1) {
            report.error(file, "jobSelectLevel",
                    "1 이상이어야 해서 기본값으로 되돌립니다: " + selectLevel);
            selectLevel = defaults.jobSelectLevel();
        }
        int tier1Level = config.getInt("tier1Level", defaults.tier1Level());
        int tier2Level = config.getInt("tier2Level", defaults.tier2Level());
        if (tier1Level <= selectLevel || tier2Level <= tier1Level) {
            report.error(file, "tier1Level / tier2Level",
                    "선택 레벨 < 1차 < 2차 순서여야 해서 기본값으로 되돌립니다: "
                            + selectLevel + " / " + tier1Level + " / " + tier2Level);
            tier1Level = defaults.tier1Level();
            tier2Level = defaults.tier2Level();
        }

        Map<String, JobDefinition> baseJobs = new LinkedHashMap<>();
        ConfigurationSection root = config.getConfigurationSection("jobs");
        if (root == null) {
            report.error(file, "jobs", "직업 정의가 없습니다. 직업 선택을 쓸 수 없습니다.");
        } else {
            int order = 0;
            for (String id : root.getKeys(false)) {
                ConfigurationSection section = root.getConfigurationSection(id);
                if (section == null) {
                    report.error(file, "jobs." + id, "형식이 맞지 않아 건너뜁니다.");
                    continue;
                }
                baseJobs.put(id, new JobDefinition(id,
                        section.getString("display", id),
                        section.getString("role", id),
                        readStatBonus(section.getConfigurationSection("statBonusPerLevel"),
                                file, "jobs." + id + ".statBonusPerLevel", report),
                        readBranches(section.getConfigurationSection("tier1"), "tier2",
                                file, "jobs." + id + ".tier1", report),
                        order++));
            }
        }

        jobs = new JobSettings(new JobTree(baseJobs), selectLevel, tier1Level, tier2Level,
                config.getString("tier1Quest", defaults.tier1Quest()),
                config.getString("tier2Quest", defaults.tier2Quest()),
                config.getBoolean("branchRevert", defaults.branchRevert()));
    }

    /** 능력치 id -> 레벨당 보정치. 능력치 이름 확인은 StatService 가 한다. */
    private Map<String, Integer> readStatBonus(ConfigurationSection section, String file,
                                               String path, ValidationReport report) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            if (stats.stat(key) == null) {
                // loadStats 가 먼저 돌기 때문에 여기서 능력치 이름을 검사할 수 있다.
                report.error(file, path + "." + key,
                        "stats.yml 에 없는 능력치라 건너뜁니다: " + key);
                continue;
            }
            int value = section.getInt(key, 0);
            if (value < 0) {
                report.error(file, path + "." + key, "음수는 둘 수 없어 건너뜁니다: " + value);
                continue;
            }
            result.put(key, value);
        }
        return result;
    }

    /**
     * 전직 분기를 읽는다. 하위 키 이름만 바꿔 1차·2차에 같은 코드를 쓴다.
     *
     * @param childKey 하위 분기가 들어 있는 키 이름. 없으면 더 내려가지 않는다
     */
    private Map<String, JobBranch> readBranches(ConfigurationSection section, String childKey,
                                                String file, String path,
                                                ValidationReport report) {
        Map<String, JobBranch> result = new LinkedHashMap<>();
        if (section == null) {
            return result;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection one = section.getConfigurationSection(id);
            if (one == null) {
                report.error(file, path + "." + id, "형식이 맞지 않아 건너뜁니다.");
                continue;
            }
            Map<String, JobBranch> children = childKey == null
                    ? Map.of()
                    : readBranches(one.getConfigurationSection(childKey), null,
                            file, path + "." + id + "." + childKey, report);
            result.put(id, new JobBranch(id, one.getString("display", id), children));
        }
        return result;
    }

    private void loadGui(ValidationReport report) {
        String file = "gui.yml";
        YamlConfiguration config = read(file, report);
        Map<String, GuiScreen> screens = new LinkedHashMap<>();
        if (config == null) {
            guiScreens = screens;
            return;
        }
        ConfigurationSection root = config.getConfigurationSection("screens");
        if (root == null) {
            report.error(file, "screens", "화면 정의가 없습니다.");
            guiScreens = screens;
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                report.error(file, "screens." + id, "형식이 맞지 않아 건너뜁니다.");
                continue;
            }
            int rows = section.getInt("rows", 3);
            if (rows < 1 || rows > 6) {
                report.error(file, "screens." + id + ".rows",
                        "1 이상 6 이하여야 해서 3으로 되돌립니다: " + rows);
                rows = 3;
            }
            Map<String, GuiIcon> icons = new LinkedHashMap<>();
            ConfigurationSection iconSection = section.getConfigurationSection("icons");
            if (iconSection != null) {
                for (String role : iconSection.getKeys(false)) {
                    ConfigurationSection one = iconSection.getConfigurationSection(role);
                    if (one == null) {
                        report.error(file, "screens." + id + ".icons." + role,
                                "형식이 맞지 않아 건너뜁니다.");
                        continue;
                    }
                    int slot = one.getInt("slot", -1);
                    if (slot < 0 || slot >= rows * 9) {
                        report.error(file, "screens." + id + ".icons." + role + ".slot",
                                "화면 크기를 벗어나 건너뜁니다: " + slot);
                        continue;
                    }
                    icons.put(role, new GuiIcon(slot, one.getString("material", null)));
                }
            }
            screens.put(id, new GuiScreen(id, section.getString("title", id), rows, icons));
        }
        guiScreens = screens;
    }

    private void loadMessages(ValidationReport report) {
        String file = "messages.yml";
        YamlConfiguration config = read(file, report);
        if (config == null) {
            return;
        }
        Map<String, String> flat = new LinkedHashMap<>();
        for (String key : config.getKeys(true)) {
            if (config.isConfigurationSection(key)) {
                continue;
            }
            Object value = config.get(key);
            if (value != null) {
                flat.put(key, String.valueOf(value));
            }
        }
        messages.replaceAll(flat);
    }

    /** 파일을 읽는다. 없거나 비어 있으면 리포트에 남기고 null 을 준다. */
    private YamlConfiguration read(String name, ValidationReport report) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.isFile()) {
            report.error(name, "", "파일이 없어 기본값을 씁니다.");
            return null;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.getKeys(false).isEmpty()) {
            report.error(name, "", "내용이 비어 있어 기본값을 씁니다.");
            return null;
        }
        return config;
    }

    /** /rpg admin status 에 쓸 파일 목록. */
    public List<String> defaultFiles() {
        return new ArrayList<>(DEFAULT_FILES);
    }
}
