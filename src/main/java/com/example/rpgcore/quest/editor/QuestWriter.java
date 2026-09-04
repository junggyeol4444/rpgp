package com.example.rpgcore.quest.editor;

import com.example.rpgcore.quest.QuestDefinition;
import com.example.rpgcore.quest.objective.Objective;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * 지시서 14장 10단계 — GUI 로 만든 퀘스트를 YAML 로 쓴다.
 *
 * <p>손으로 쓴 quests.yml 을 건드리지 않도록 quests/editor.yml 에만 쓴다.
 * 그 디렉터리는 지시서 6장의 스캔 대상이라 다음 리로드에 함께 읽힌다.
 *
 * <p>파일에 닿는 메서드({@link #refresh()} · {@link #write} · {@link #delete})
 * 는 IO 스레드에서만 부른다. (지시서 0장 4번) 화면이 그릴 때 쓰는
 * {@link #managedIds()} 는 파일을 읽지 않고 마지막으로 읽어둔 목록만 준다.
 */
public final class QuestWriter {

    /** GUI 가 관리하는 파일. 손으로 쓴 quests.yml 과 섞지 않는다. */
    public static final String EDITOR_FILE = "quests/editor.yml";

    private final File file;

    /**
     * 이 파일이 관리하는 퀘스트 id.
     *
     * <p>화면이 매번 그릴 때마다 읽으면 메인 스레드에서 파일을 열게 된다.
     * 그래서 파일에 닿는 쪽에서만 갈아 끼우고, 화면은 이 값만 본다.
     * 갈아 끼울 때 통째로 바꾸므로 volatile 하나면 충분하다.
     */
    private volatile Set<String> managedIds = Set.of();

    public QuestWriter(File pluginDataFolder) {
        this.file = new File(pluginDataFolder, EDITOR_FILE);
    }

    public File file() {
        return file;
    }

    /** 이 파일이 관리하는 퀘스트 id. 파일을 읽지 않는다. */
    public Set<String> managedIds() {
        return managedIds;
    }

    /** 파일을 읽어 관리 목록을 다시 채운다. IO 스레드 전용. */
    public void refresh() {
        managedIds = readIds(load());
    }

    /**
     * 퀘스트 하나를 넣거나 덮어쓴다.
     *
     * <p>같은 파일 안의 다른 퀘스트는 그대로 둔다.
     *
     * @throws IOException 파일을 쓰지 못했을 때
     */
    public void write(QuestDefinition quest) throws IOException {
        YamlConfiguration config = load();
        config.set("quests." + quest.id(), toMap(quest));
        save(config);
    }

    /** 퀘스트 하나를 지운다. */
    public void delete(String questId) throws IOException {
        YamlConfiguration config = load();
        config.set("quests." + questId, null);
        save(config);
    }

    private YamlConfiguration load() {
        return file.isFile()
                ? YamlConfiguration.loadConfiguration(file)
                : new YamlConfiguration();
    }

    private static Set<String> readIds(YamlConfiguration config) {
        var section = config.getConfigurationSection("quests");
        return section == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(section.getKeys(false)));
    }

    private void save(YamlConfiguration config) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("디렉터리를 만들지 못했습니다: " + parent.getAbsolutePath());
        }
        config.options().setHeader(List.of(
                "이 파일은 /rpg admin questedit 가 관리한다.",
                "직접 고쳐도 되지만, GUI 로 같은 퀘스트를 저장하면 덮어쓴다.",
                "손으로만 관리할 퀘스트는 quests.yml 에 두는 편이 낫다."));
        config.save(file);
        managedIds = readIds(config);
    }

    /** 지시서 8장 [quests.yml] 모양 그대로 만든다. */
    private static Map<String, Object> toMap(QuestDefinition quest) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("display", quest.display());
        map.put("type", quest.type().name());
        map.put("requireLevel", quest.requireLevel());
        if (quest.requireJob() != null) {
            map.put("requireJob", quest.requireJob());
        }

        List<Map<String, Object>> objectives = new ArrayList<>();
        for (Objective objective : quest.objectives()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", objective.type().name());
            entry.put(objective.type().keyField(), objective.key());
            entry.put("amount", objective.amount());
            objectives.add(entry);
        }
        map.put("objectives", objectives);

        Map<String, Object> rewards = new LinkedHashMap<>();
        if (quest.reward().combatExp() > 0) {
            rewards.put("combatExp", quest.reward().combatExp());
        }
        if (quest.reward().skillPoints() > 0) {
            rewards.put("skillPoints", quest.reward().skillPoints());
        }
        if (quest.reward().statPoints() > 0) {
            rewards.put("statPoints", quest.reward().statPoints());
        }
        if (!quest.reward().currency().isEmpty()) {
            rewards.put("currency", new LinkedHashMap<>(quest.reward().currency()));
        }
        map.put("rewards", rewards);

        map.put("repeatable", quest.repeatable());
        return map;
    }
}
