import com.example.rpgcore.config.schema.*;
import com.example.rpgcore.life.*;
import com.example.rpgcore.life.unlock.TrackReward;
import java.util.*;

public class Check6 {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok, Object got) {
        if (ok) { pass++; System.out.println("  PASS  " + n); }
        else { fail++; System.out.println("  FAIL  " + n + "  (got: " + got + ")"); }
    }

    public static void main(String[] a) {
        System.out.println("[1] TrackDefinition — 대상별 값과 기본값");
        Map<LifeSource, Map<String, Double>> sources = new EnumMap<>(LifeSource.class);
        sources.put(LifeSource.BLOCK_BREAK, Map.of("OAK_LOG", 5.0, "WHEAT", 3.0));
        sources.put(LifeSource.FISHING, Map.of(TrackDefinition.DEFAULT_KEY, 8.0));
        TrackDefinition living = new TrackDefinition(TrackType.LIVING, "생활", sources);

        check("대상별 값", living.exp(LifeSource.BLOCK_BREAK, "OAK_LOG") == 5.0, "");
        check("표에 없는 대상은 0", living.exp(LifeSource.BLOCK_BREAK, "STONE") == 0.0, "");
        check("대상 구분 없는 획득원", living.exp(LifeSource.FISHING, null) == 8.0, "");
        check("대상을 넘겨도 기본값으로", living.exp(LifeSource.FISHING, "COD") == 8.0, "");
        check("안 쓰는 획득원은 0", living.exp(LifeSource.BREW, "POTION") == 0.0, "");
        check("handles 판정",
                living.handles(LifeSource.BLOCK_BREAK) && !living.handles(LifeSource.CRAFT), "");
        try {
            living.sources().get(LifeSource.FISHING).put("X", 1.0);
            check("sources 불변", false, "수정됨");
        } catch (UnsupportedOperationException e) {
            check("sources 불변", true, "");
        }

        System.out.println();
        System.out.println("[2] TrackReward — 효율과 해금 (기획서 3장)");
        Map<Integer, List<String>> unlocks = new LinkedHashMap<>();
        unlocks.put(5, List.of("recipe_a"));
        unlocks.put(10, List.of("recipe_b", "recipe_c"));
        TrackReward reward = new TrackReward(Map.of("gatherSpeed", 0.005), unlocks);

        check("레벨당 효율", reward.efficiency("gatherSpeed", 20) == 0.1,
                reward.efficiency("gatherSpeed", 20));
        check("없는 효율은 0", reward.efficiency("nope", 20) == 0.0, "");
        check("음수 레벨은 0으로", reward.efficiency("gatherSpeed", -5) == 0.0, "");
        check("4레벨엔 해금 없음", reward.unlockedUpTo(4).isEmpty(), reward.unlockedUpTo(4));
        check("5레벨엔 1개", reward.unlockedUpTo(5).equals(List.of("recipe_a")),
                reward.unlockedUpTo(5));
        check("레벨을 건너뛰어도 이하 전부",
                reward.unlockedUpTo(50).size() == 3, reward.unlockedUpTo(50));
        check("none 은 비어 있음",
                TrackReward.none().unlockedUpTo(99).isEmpty(), "");

        System.out.println();
        System.out.println("[3] LifeSettings");
        Map<TrackType, TrackDefinition> tracks = new EnumMap<>(TrackType.class);
        tracks.put(TrackType.LIVING, living);
        LifeSettings settings = new LifeSettings(tracks,
                new EnumMap<>(Map.of(TrackType.LIVING, reward)));
        check("트랙 조회", settings.track(TrackType.LIVING) == living, "");
        check("정의 없는 트랙은 null", settings.track(TrackType.MINING) == null, "");
        check("보상 없는 트랙은 빈 보상",
                settings.reward(TrackType.MINING).unlockedUpTo(99).isEmpty(), "");
        check("defaults 는 비어 있음", LifeSettings.defaults().tracks().isEmpty(), "");
        check("LifeSource 파싱",
                LifeSource.fromConfig("block_break") == LifeSource.BLOCK_BREAK, "");
        check("모르는 획득원은 null", LifeSource.fromConfig("nope") == null, "");

        System.out.println();
        System.out.println("[4] CurrencyDefinition — 상한 (6단계)");
        CurrencyDefinition capped = new CurrencyDefinition("coin", "코인", 100);
        CurrencyDefinition free = new CurrencyDefinition("coin", "코인", -1);
        check("상한 있음", capped.hasMax() && !free.hasMax(), "");
        check("상한까지 자름", capped.clamp(150) == 100, capped.clamp(150));
        check("상한 아래는 그대로", capped.clamp(50) == 50, "");
        check("음수는 0으로", capped.clamp(-5) == 0, "");
        check("상한 없으면 그대로", free.clamp(Long.MAX_VALUE) == Long.MAX_VALUE, "");

        System.out.println();
        System.out.println("[5] EconomySettings");
        EconomySettings economy = new EconomySettings(true, true,
                Map.of("dungeon_coin", capped));
        check("재화 조회", economy.currency("dungeon_coin") == capped, "");
        check("없는 재화는 null", economy.currency("nope") == null, "");
        check("null 도 null", economy.currency(null) == null, "");
        check("defaults 는 재화 없음", EconomySettings.defaults().currencies().isEmpty(), "");

        System.out.println();
        System.out.println("결과: " + pass + " 통과 / " + fail + " 실패");
        if (fail > 0) System.exit(1);
    }
}
