package com.example.rpgcore.storage.yaml;

import com.example.rpgcore.config.schema.StorageSettings;
import com.example.rpgcore.config.validation.ValidationReport;
import com.example.rpgcore.player.data.PlayerData;
import com.example.rpgcore.player.data.PlayerDataCodec;
import com.example.rpgcore.storage.PlayerDataRepository;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * 지시서 5장 [YAML 구현].
 *
 * <p>파일 경로: plugins/RpgCore/playerdata/&lt;uuid&gt;.yml
 *
 * <p>YAML 관련 타입은 이 클래스 밖으로 나가지 않는다. 바깥과는
 * {@link PlayerData} 로만 주고받는다.
 *
 * <p>[확인 완료] {@link YamlConfiguration} 은 26.1.2 에 그대로 있다.
 * Paper 26.1.2 API 소스로 컴파일해 확인했다. (tools/verify-against-paper.sh)
 * 플러그인에서 이 API를 쓰는 곳은 이 클래스와 ConfigManager 두 곳뿐이다.
 */
public final class YamlRepository implements PlayerDataRepository {

    private final File directory;
    private final Logger logger;

    public YamlRepository(File pluginDataFolder, Logger logger) {
        this.directory = new File(pluginDataFolder, "playerdata");
        this.logger = logger;
        if (!directory.exists() && !directory.mkdirs()) {
            logger.warning("playerdata 디렉터리를 만들지 못했습니다: " + directory.getAbsolutePath());
        }
    }

    @Override
    public String storageType() {
        return StorageSettings.YAML;
    }

    @Override
    public PlayerData load(UUID uuid) {
        File file = fileOf(uuid);
        if (!file.isFile()) {
            return new PlayerData(uuid);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ValidationReport report = new ValidationReport();
        PlayerData data = PlayerDataCodec.fromMap(uuid, sectionToMap(config), report);
        logReport(file.getName(), report);
        return data;
    }

    @Override
    public void save(PlayerData data) {
        File file = fileOf(data.uuid());
        YamlConfiguration config = new YamlConfiguration();
        writeMap(config, PlayerDataCodec.toMap(data));

        // 쓰다가 서버가 죽어도 기존 파일이 반쯤 덮이지 않도록 임시 파일에
        // 먼저 쓰고 옮긴다.
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            config.save(temp);
            Files.move(temp.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "플레이어 데이터를 저장하지 못했습니다: " + file.getName(), e);
            if (temp.exists() && !temp.delete()) {
                logger.warning("임시 파일을 지우지 못했습니다: " + temp.getName());
            }
        }
    }

    @Override
    public void saveAll() {
        // 이 구현은 save() 안에서 곧바로 디스크에 쓰므로 남아 있는 쓰기가 없다.
        // 쓰기를 모아두는 구현(SQLite 등)으로 바뀌면 여기서 마무리한다.
    }

    @Override
    public boolean exists(UUID uuid) {
        return fileOf(uuid).isFile();
    }

    @Override
    public void delete(UUID uuid) {
        File file = fileOf(uuid);
        if (file.isFile() && !file.delete()) {
            logger.warning("플레이어 데이터를 지우지 못했습니다: " + file.getName());
        }
    }

    private File fileOf(UUID uuid) {
        return new File(directory, uuid + ".yml");
    }

    private void logReport(String fileName, ValidationReport report) {
        for (ValidationReport.Entry entry : report.entries()) {
            logger.warning(entry.toString());
        }
        if (!report.isEmpty()) {
            logger.warning(fileName + " 에서 " + report.size() + "건의 문제를 건너뛰었습니다.");
        }
    }

    // ------------------------------------------------------------
    // ConfigurationSection <-> Map
    // ------------------------------------------------------------

    /** 설정 구역을 중첩 Map 으로 바꾼다. 하위 구역도 Map 으로 내려간다. */
    static Map<String, Object> sectionToMap(ConfigurationSection section) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection child) {
                result.put(key, sectionToMap(child));
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    /** 중첩 Map 을 설정 구역에 써 넣는다. */
    static void writeMap(ConfigurationSection section, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> child) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typed = (Map<String, Object>) child;
                writeMap(section.createSection(entry.getKey()), typed);
            } else if (value != null) {
                // null 은 키를 지우는 것과 같으므로 쓰지 않는다.
                // 읽을 때 없는 키는 null 로 해석되어 의미가 같다.
                section.set(entry.getKey(), value);
            }
        }
    }
}
