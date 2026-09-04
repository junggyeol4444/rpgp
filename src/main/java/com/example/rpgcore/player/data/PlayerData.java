package com.example.rpgcore.player.data;

import com.example.rpgcore.binding.HoldState;
import com.example.rpgcore.binding.InputTrigger;
import com.example.rpgcore.life.TrackType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 지시서 7장 [플레이어 데이터 스키마] 의 자바 표현. 저장 대상이다.
 *
 * <p>저장소 구현(YAML 등)에 의존하지 않는다. 지시서 5장 [교체 가능성]
 * 에 따라 이 클래스에는 YAML 관련 타입이 들어오지 않는다.
 * 직렬화는 {@link PlayerDataCodec} 가 Map 형태로만 처리한다.
 *
 * <p>[경험치를 double 로 두는 이유]
 * 전투 레벨과 생활 트랙 모두 상한이 없고(levels.yml maxLevel: -1),
 * 요구 경험치가 지수형이라 고레벨에서 long 범위를 넘어선다.
 * 예: base 100 / factor 1.12 기준 1000레벨 요구치는 약 1.9e51 이다.
 * 따라서 정수형이 아니라 double 로 둔다.
 */
public final class PlayerData {

    private final UUID uuid;
    private String name;
    private long lastLogin;

    private final Combat combat = new Combat();
    private final Life life = new Life();
    private final Job job = new Job();
    private final Skill skill = new Skill();
    private final Binding binding = new Binding();
    private final Quest quest = new Quest();

    /** 특수 재화. 재화 id -> 보유량. (economy.yml) */
    private final Map<String, Long> currency = new LinkedHashMap<>();

    /**
     * 지시서 7장 [규칙]: 알 수 없는 키는 버리지 않고 보존한다.
     * 버전 간 데이터 손실을 막기 위한 것이다. 저장할 때 그대로 다시 쓴다.
     */
    private final Map<String, Object> unknown = new LinkedHashMap<>();

    /** 지시서 4장 [변경 추적]. 저장이 끝나면 내려간다. */
    private transient boolean dirty;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    // ------------------------------------------------------------
    // 최상위
    // ------------------------------------------------------------

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        this.name = name;
    }

    public long lastLogin() {
        return lastLogin;
    }

    public void lastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Combat combat() {
        return combat;
    }

    public Life life() {
        return life;
    }

    public Job job() {
        return job;
    }

    public Skill skill() {
        return skill;
    }

    public Binding binding() {
        return binding;
    }

    public Quest quest() {
        return quest;
    }

    public Map<String, Long> currency() {
        return currency;
    }

    public long currency(String currencyId) {
        Long value = currency.get(currencyId);
        return value == null ? 0L : value;
    }

    public Map<String, Object> unknown() {
        return unknown;
    }

    // ------------------------------------------------------------
    // 변경 추적
    // ------------------------------------------------------------

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void clearDirty() {
        this.dirty = false;
    }

    // ------------------------------------------------------------
    // combat:
    // ------------------------------------------------------------

    /** 전투 레벨·경험치·스탯. */
    public static final class Combat {

        private int level = 1;
        private double exp;
        private int statPoints;
        /** 능력치 id -> 분배한 포인트. 능력치 종류는 stats.yml 에서 정의한다. */
        private final Map<String, Integer> stats = new LinkedHashMap<>();
        private int statResetCount;

        public int level() {
            return level;
        }

        public void level(int level) {
            this.level = level;
        }

        public double exp() {
            return exp;
        }

        public void exp(double exp) {
            this.exp = exp;
        }

        public int statPoints() {
            return statPoints;
        }

        public void statPoints(int statPoints) {
            this.statPoints = statPoints;
        }

        public Map<String, Integer> stats() {
            return stats;
        }

        public int stat(String statId) {
            Integer value = stats.get(statId);
            return value == null ? 0 : value;
        }

        public int statResetCount() {
            return statResetCount;
        }

        public void statResetCount(int statResetCount) {
            this.statResetCount = statResetCount;
        }
    }

    // ------------------------------------------------------------
    // life:
    // ------------------------------------------------------------

    /** 생활 4트랙. 7단계에서 실제로 오르기 시작한다. */
    public static final class Life {

        private final Map<TrackType, Track> tracks = new EnumMap<>(TrackType.class);
        /** 해금된 레시피·콘텐츠 id. */
        private final List<String> unlocked = new ArrayList<>();

        public Life() {
            for (TrackType type : TrackType.values()) {
                tracks.put(type, new Track());
            }
        }

        public Track track(TrackType type) {
            return tracks.get(type);
        }

        public Map<TrackType, Track> tracks() {
            return tracks;
        }

        public List<String> unlocked() {
            return unlocked;
        }
    }

    /** 트랙 하나의 레벨·경험치. */
    public static final class Track {

        private int level = 1;
        private double exp;

        public int level() {
            return level;
        }

        public void level(int level) {
            this.level = level;
        }

        public double exp() {
            return exp;
        }

        public void exp(double exp) {
            this.exp = exp;
        }
    }

    // ------------------------------------------------------------
    // job:
    // ------------------------------------------------------------

    /**
     * 직업 상태. 세 값 모두 null 이면 미선택이다.
     * 지시서 7장 [규칙]: job.base 가 null 이면 직업 미선택 상태로 취급한다.
     */
    public static final class Job {

        private String base;
        private String tier1;
        private String tier2;

        public String base() {
            return base;
        }

        public void base(String base) {
            this.base = base;
        }

        public String tier1() {
            return tier1;
        }

        public void tier1(String tier1) {
            this.tier1 = tier1;
        }

        public String tier2() {
            return tier2;
        }

        public void tier2(String tier2) {
            this.tier2 = tier2;
        }

        public boolean hasBase() {
            return base != null;
        }

        /** 전직 단계. 0 = 미선택, 1 = 기본, 2 = 1차, 3 = 2차. */
        public int stage() {
            if (base == null) {
                return 0;
            }
            if (tier1 == null) {
                return 1;
            }
            return tier2 == null ? 2 : 3;
        }
    }

    // ------------------------------------------------------------
    // skill:
    // ------------------------------------------------------------

    /** 스킬 해금·레벨·포인트. 4단계에서 쓰인다. */
    public static final class Skill {

        private int points;
        private final Set<String> unlocked = new LinkedHashSet<>();
        private final Map<String, Integer> levels = new LinkedHashMap<>();

        public int points() {
            return points;
        }

        public void points(int points) {
            this.points = points;
        }

        public Set<String> unlocked() {
            return unlocked;
        }

        public Map<String, Integer> levels() {
            return levels;
        }

        /**
         * 지시서 7장 [규칙]: skill.levels 에 없는 해금 스킬은 레벨 1로 본다.
         *
         * @return 해금하지 않은 스킬이면 0
         */
        public int levelOf(String skillId) {
            if (!unlocked.contains(skillId)) {
                return 0;
            }
            Integer level = levels.get(skillId);
            return level == null ? 1 : level;
        }
    }

    // ------------------------------------------------------------
    // binding:
    // ------------------------------------------------------------

    /** 스킬 등록 상태. 4단계에서 쓰인다. */
    public static final class Binding {

        /**
         * 슬롯 인덱스 순서대로 스킬 id. 비어 있으면 null 이 들어간다.
         * 슬롯 수는 시작 2칸, 전직마다 +1 이다. (지시서 10장)
         */
        private final List<String> itemSlots = new ArrayList<>();
        private final List<KeyCombo> keyCombos = new ArrayList<>();

        public List<String> itemSlots() {
            return itemSlots;
        }

        public List<KeyCombo> keyCombos() {
            return keyCombos;
        }
    }

    /**
     * 키 조합 하나. 조합 = 유지 상태 1개 + 순간 입력 1개.
     * 같은 (hold, trigger) 조합은 중복 등록할 수 없다. (지시서 13장)
     */
    public record KeyCombo(HoldState hold, InputTrigger trigger, String skillId) {
    }

    // ------------------------------------------------------------
    // quest:
    // ------------------------------------------------------------

    /** 퀘스트 진행 상태. */
    public static final class Quest {

        /**
         * 퀘스트 id -&gt; 진행 상태.
         *
         * <p>안에 들어가는 모양은 5단계에서
         * {@link com.example.rpgcore.quest.QuestProgress} 로 확정했다.
         * 여기서 {@code Object} 로 두는 것은 저장 계층이 퀘스트의 뜻을
         * 몰라도 되게 하기 위해서다. 읽고 쓰는 것은 QuestProgress 가 한다.
         */
        private final Map<String, Object> active = new LinkedHashMap<>();
        private final List<String> completed = new ArrayList<>();
        private long dailyResetAt;
        private long weeklyResetAt;

        public Map<String, Object> active() {
            return active;
        }

        public List<String> completed() {
            return completed;
        }

        public long dailyResetAt() {
            return dailyResetAt;
        }

        public void dailyResetAt(long dailyResetAt) {
            this.dailyResetAt = dailyResetAt;
        }

        public long weeklyResetAt() {
            return weeklyResetAt;
        }

        public void weeklyResetAt(long weeklyResetAt) {
            this.weeklyResetAt = weeklyResetAt;
        }
    }
}
