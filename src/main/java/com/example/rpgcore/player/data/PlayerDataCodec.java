package com.example.rpgcore.player.data;

import com.example.rpgcore.binding.HoldState;
import com.example.rpgcore.binding.InputTrigger;
import com.example.rpgcore.config.validation.ValidationReport;
import com.example.rpgcore.life.TrackType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@link PlayerData} 와 중첩 Map 사이의 변환기.
 *
 * <p>여기서 다루는 것은 순수 Map 뿐이다. YAML·SQL 어느 쪽에도 의존하지
 * 않으므로 저장소를 갈아끼워도 이 파일은 그대로 쓸 수 있다.
 * (지시서 5장 [교체 가능성])
 *
 * <p>지시서 7장 [규칙]에 따라 아래 {@link #KNOWN_PATHS} 에 없는 키는
 * 전부 {@link PlayerData#unknown()} 으로 옮겨 보존하고, 저장할 때 다시
 * 써 넣는다.
 */
public final class PlayerDataCodec {

    /**
     * 이 코덱이 해석하는 경로. 여기에 없는 키는 알 수 없는 키로 보존된다.
     * 하위 키가 동적으로 늘어나는 경로(combat.stats, currency 등)는
     * 하위 전체를 하나의 경로로 잡는다.
     */
    private static final List<String> KNOWN_PATHS = List.of(
            "uuid",
            "name",
            "lastLogin",
            "combat.level",
            "combat.exp",
            "combat.statPoints",
            "combat.stats",
            "combat.statResetCount",
            "life.living.level", "life.living.exp",
            "life.mining.level", "life.mining.exp",
            "life.crafting.level", "life.crafting.exp",
            "life.alchemy.level", "life.alchemy.exp",
            "life.unlocked",
            "job.base",
            "job.tier1",
            "job.tier2",
            "skill.points",
            "skill.unlocked",
            "skill.levels",
            "binding.itemSlots",
            "binding.keyCombos",
            "quest.active",
            "quest.completed",
            "quest.dailyResetAt",
            "quest.weeklyResetAt",
            "currency");

    private PlayerDataCodec() {
    }

    // ================================================================
    // 읽기
    // ================================================================

    /**
     * Map 을 PlayerData 로 읽는다.
     *
     * <p>값이 잘못되었으면 예외를 던지지 않고 기본값을 쓴 뒤 리포트에
     * 남긴다. (지시서 6장: 서버를 죽이지 않는다)
     *
     * @param uuid   대상 플레이어
     * @param root   저장 파일에서 읽은 중첩 Map. null 이면 신규 데이터
     * @param report 문제를 기록할 리포트. null 허용
     */
    public static PlayerData fromMap(UUID uuid, Map<String, Object> root, ValidationReport report) {
        PlayerData data = new PlayerData(uuid);
        if (root == null || root.isEmpty()) {
            return data;
        }
        String file = uuid + ".yml";

        data.name(asString(get(root, "name"), null));
        data.lastLogin(asLong(get(root, "lastLogin"), 0L, file, "lastLogin", report));

        readCombat(data, root, file, report);
        readLife(data, root, file, report);
        readJob(data, root);
        readSkill(data, root, file, report);
        readBinding(data, root, file, report);
        readQuest(data, root, file, report);
        readCurrency(data, root, file, report);

        // 남은 키를 전부 보존한다.
        Map<String, Object> leftover = deepCopyMap(root);
        for (String path : KNOWN_PATHS) {
            removePath(leftover, path);
        }
        prune(leftover);
        data.unknown().putAll(leftover);

        data.clearDirty();
        return data;
    }

    private static void readCombat(PlayerData data, Map<String, Object> root,
                                   String file, ValidationReport report) {
        PlayerData.Combat combat = data.combat();
        combat.level(asInt(get(root, "combat.level"), 1, file, "combat.level", report));
        combat.exp(asDouble(get(root, "combat.exp"), 0.0, file, "combat.exp", report));
        combat.statPoints(asInt(get(root, "combat.statPoints"), 0, file, "combat.statPoints", report));
        combat.statResetCount(
                asInt(get(root, "combat.statResetCount"), 0, file, "combat.statResetCount", report));

        Map<String, Object> stats = asMap(get(root, "combat.stats"));
        if (stats != null) {
            for (Map.Entry<String, Object> entry : stats.entrySet()) {
                combat.stats().put(entry.getKey(),
                        asInt(entry.getValue(), 0, file, "combat.stats." + entry.getKey(), report));
            }
        }
        if (combat.level() < 1) {
            report(report, file, "combat.level", "레벨이 1보다 작아 1로 되돌립니다: " + combat.level());
            combat.level(1);
        }
    }

    private static void readLife(PlayerData data, Map<String, Object> root,
                                 String file, ValidationReport report) {
        for (TrackType type : TrackType.values()) {
            String base = "life." + type.configKey();
            PlayerData.Track track = data.life().track(type);
            track.level(asInt(get(root, base + ".level"), 1, file, base + ".level", report));
            track.exp(asDouble(get(root, base + ".exp"), 0.0, file, base + ".exp", report));
            if (track.level() < 1) {
                report(report, file, base + ".level", "레벨이 1보다 작아 1로 되돌립니다.");
                track.level(1);
            }
        }
        data.life().unlocked().addAll(asStringList(get(root, "life.unlocked")));
    }

    private static void readJob(PlayerData data, Map<String, Object> root) {
        data.job().base(asString(get(root, "job.base"), null));
        data.job().tier1(asString(get(root, "job.tier1"), null));
        data.job().tier2(asString(get(root, "job.tier2"), null));
    }

    private static void readSkill(PlayerData data, Map<String, Object> root,
                                  String file, ValidationReport report) {
        PlayerData.Skill skill = data.skill();
        skill.points(asInt(get(root, "skill.points"), 0, file, "skill.points", report));
        skill.unlocked().addAll(asStringList(get(root, "skill.unlocked")));

        Map<String, Object> levels = asMap(get(root, "skill.levels"));
        if (levels != null) {
            for (Map.Entry<String, Object> entry : levels.entrySet()) {
                skill.levels().put(entry.getKey(),
                        asInt(entry.getValue(), 1, file, "skill.levels." + entry.getKey(), report));
            }
        }
    }

    private static void readBinding(PlayerData data, Map<String, Object> root,
                                    String file, ValidationReport report) {
        PlayerData.Binding binding = data.binding();

        Object slots = get(root, "binding.itemSlots");
        if (slots instanceof List<?> list) {
            for (Object entry : list) {
                // 빈 슬롯은 null 로 유지한다.
                binding.itemSlots().add(entry == null ? null : String.valueOf(entry));
            }
        }

        Object combos = get(root, "binding.keyCombos");
        if (combos instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                Map<String, Object> entry = asMap(list.get(i));
                String path = "binding.keyCombos[" + i + "]";
                if (entry == null) {
                    report(report, file, path, "형식이 맞지 않아 건너뜁니다.");
                    continue;
                }
                HoldState hold = parseEnum(HoldState.class, asString(entry.get("hold"), null));
                InputTrigger trigger =
                        parseEnum(InputTrigger.class, asString(entry.get("trigger"), null));
                String skillId = asString(entry.get("skill"), null);
                if (hold == null || trigger == null || skillId == null) {
                    report(report, file, path, "hold/trigger/skill 값을 해석할 수 없어 건너뜁니다.");
                    continue;
                }
                binding.keyCombos().add(new PlayerData.KeyCombo(hold, trigger, skillId));
            }
        }
    }

    private static void readQuest(PlayerData data, Map<String, Object> root,
                                  String file, ValidationReport report) {
        PlayerData.Quest quest = data.quest();
        Map<String, Object> active = asMap(get(root, "quest.active"));
        if (active != null) {
            // 진행 상태의 구조는 아직 확정되지 않았으므로 값을 그대로 보존한다.
            quest.active().putAll(deepCopyMap(active));
        }
        quest.completed().addAll(asStringList(get(root, "quest.completed")));
        quest.dailyResetAt(
                asLong(get(root, "quest.dailyResetAt"), 0L, file, "quest.dailyResetAt", report));
        quest.weeklyResetAt(
                asLong(get(root, "quest.weeklyResetAt"), 0L, file, "quest.weeklyResetAt", report));
    }

    private static void readCurrency(PlayerData data, Map<String, Object> root,
                                     String file, ValidationReport report) {
        Map<String, Object> currency = asMap(get(root, "currency"));
        if (currency == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : currency.entrySet()) {
            data.currency().put(entry.getKey(),
                    asLong(entry.getValue(), 0L, file, "currency." + entry.getKey(), report));
        }
    }

    // ================================================================
    // 쓰기
    // ================================================================

    /** PlayerData 를 저장용 중첩 Map 으로 만든다. 보존해 둔 키를 먼저 깐다. */
    public static Map<String, Object> toMap(PlayerData data) {
        Map<String, Object> root = deepCopyMap(data.unknown());

        put(root, "uuid", data.uuid().toString());
        put(root, "name", data.name());
        put(root, "lastLogin", data.lastLogin());

        PlayerData.Combat combat = data.combat();
        put(root, "combat.level", combat.level());
        put(root, "combat.exp", combat.exp());
        put(root, "combat.statPoints", combat.statPoints());
        put(root, "combat.stats", new LinkedHashMap<String, Object>(combat.stats()));
        put(root, "combat.statResetCount", combat.statResetCount());

        for (TrackType type : TrackType.values()) {
            PlayerData.Track track = data.life().track(type);
            put(root, "life." + type.configKey() + ".level", track.level());
            put(root, "life." + type.configKey() + ".exp", track.exp());
        }
        put(root, "life.unlocked", new ArrayList<>(data.life().unlocked()));

        put(root, "job.base", data.job().base());
        put(root, "job.tier1", data.job().tier1());
        put(root, "job.tier2", data.job().tier2());

        put(root, "skill.points", data.skill().points());
        put(root, "skill.unlocked", new ArrayList<>(data.skill().unlocked()));
        put(root, "skill.levels", new LinkedHashMap<String, Object>(data.skill().levels()));

        List<Object> slots = new ArrayList<>(data.binding().itemSlots());
        put(root, "binding.itemSlots", slots);

        List<Object> combos = new ArrayList<>();
        for (PlayerData.KeyCombo combo : data.binding().keyCombos()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("hold", combo.hold().name());
            entry.put("trigger", combo.trigger().name());
            entry.put("skill", combo.skillId());
            combos.add(entry);
        }
        put(root, "binding.keyCombos", combos);

        put(root, "quest.active", deepCopyMap(data.quest().active()));
        put(root, "quest.completed", new ArrayList<>(data.quest().completed()));
        put(root, "quest.dailyResetAt", data.quest().dailyResetAt());
        put(root, "quest.weeklyResetAt", data.quest().weeklyResetAt());

        put(root, "currency", new LinkedHashMap<String, Object>(data.currency()));

        return root;
    }

    // ================================================================
    // 경로 다루기
    // ================================================================

    static Object get(Map<String, Object> root, String path) {
        String[] parts = path.split("\\.");
        Object current = root;
        for (String part : parts) {
            Map<String, Object> map = asMap(current);
            if (map == null) {
                return null;
            }
            current = map.get(part);
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    static void put(Map<String, Object> root, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Object child = current.get(parts[i]);
            if (!(child instanceof Map)) {
                child = new LinkedHashMap<String, Object>();
                current.put(parts[i], child);
            }
            current = (Map<String, Object>) child;
        }
        current.put(parts[parts.length - 1], value);
    }

    /**
     * 경로에 있는 키를 지운다.
     *
     * <p>{@link #asMap(Object)} 는 사본을 만들기 때문에 여기서는 쓸 수 없다.
     * 사본을 고치면 원본은 그대로 남는다. 반드시 중첩 Map 을 직접 타고 들어가
     * 그 자리에서 지워야 한다.
     *
     * <p>호출 전제: root 는 {@link #deepCopyMap(Map)} 를 거친 Map 이라
     * 모든 하위 Map 의 키가 String 이다.
     */
    @SuppressWarnings("unchecked")
    static void removePath(Map<String, Object> root, String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Object child = current.get(parts[i]);
            if (!(child instanceof Map)) {
                return;
            }
            current = (Map<String, Object>) child;
        }
        current.remove(parts[parts.length - 1]);
    }

    /**
     * 알 수 없는 키만 남기고 나니 빈 껍데기가 된 Map 을 지운다.
     * removePath 와 같은 이유로 여기서도 사본을 만들지 않는다.
     */
    @SuppressWarnings("unchecked")
    static void prune(Map<String, Object> root) {
        root.entrySet().removeIf(entry -> {
            if (!(entry.getValue() instanceof Map)) {
                return false;
            }
            Map<String, Object> child = (Map<String, Object>) entry.getValue();
            prune(child);
            return child.isEmpty();
        });
    }

    // ================================================================
    // 값 변환
    // ================================================================

    /**
     * Map 으로 읽는다. 키를 String 으로 맞추기 위해 <b>사본</b>을 만든다.
     * 값을 고치는 용도로 쓰면 안 된다. (읽기 전용)
     */
    static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return null;
    }

    static Map<String, Object> deepCopyMap(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (source == null) {
            return copy;
        }
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            copy.put(entry.getKey(), deepCopy(entry.getValue()));
        }
        return copy;
    }

    static Object deepCopy(Object value) {
        Map<String, Object> map = asMap(value);
        if (map != null) {
            return deepCopyMap(map);
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object entry : list) {
                copy.add(deepCopy(entry));
            }
            return copy;
        }
        return value;
    }

    static String asString(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    static List<String> asStringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object entry : list) {
                if (entry != null) {
                    result.add(String.valueOf(entry));
                }
            }
        }
        return result;
    }

    static int asInt(Object value, int fallback, String file, String path, ValidationReport report) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            report(report, file, path, "정수가 아니어서 기본값 " + fallback + " 을 씁니다: " + value);
        }
        return fallback;
    }

    static long asLong(Object value, long fallback, String file, String path, ValidationReport report) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            report(report, file, path, "정수가 아니어서 기본값 " + fallback + " 을 씁니다: " + value);
        }
        return fallback;
    }

    static double asDouble(Object value, double fallback, String file, String path,
                           ValidationReport report) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null) {
            report(report, file, path, "숫자가 아니어서 기본값 " + fallback + " 을 씁니다: " + value);
        }
        return fallback;
    }

    static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (value == null) {
            return null;
        }
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(value)) {
                return constant;
            }
        }
        return null;
    }

    private static void report(ValidationReport report, String file, String path, String reason) {
        if (report != null) {
            report.warn(file, path, reason);
        }
    }
}
