package com.example.rpgcore.config;

import com.example.rpgcore.config.schema.CurveSettings;
import com.example.rpgcore.config.schema.GeneralSettings;
import com.example.rpgcore.config.schema.LevelSettings;
import com.example.rpgcore.config.schema.StorageSettings;
import com.example.rpgcore.config.validation.ValidationReport;
import com.example.rpgcore.core.Reloadable;
import com.example.rpgcore.level.ExpSource;
import com.example.rpgcore.util.Messages;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
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
