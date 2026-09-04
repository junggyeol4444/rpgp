import com.example.rpgcore.config.schema.*;
import com.example.rpgcore.stat.*;
import java.util.*;

public class Check2 {
    static int pass = 0, fail = 0;
    static void check(String name, boolean ok, Object got) {
        if (ok) { pass++; System.out.println("  PASS  " + name); }
        else { fail++; System.out.println("  FAIL  " + name + "  (got: " + got + ")"); }
    }

    public static void main(String[] a) {
        System.out.println("[1] CombatSettings.applyDefense (c=100, min=1)");
        CombatSettings c = CombatSettings.defaults();
        check("defense 0   -> 100", c.applyDefense(100, 0) == 100.0, c.applyDefense(100, 0));
        check("defense 100 -> 50",  c.applyDefense(100, 100) == 50.0, c.applyDefense(100, 100));
        check("defense 900 -> 10",  c.applyDefense(100, 900) == 10.0, c.applyDefense(100, 900));
        check("minimumDamage floor", c.applyDefense(0.5, 0) == 1.0, c.applyDefense(0.5, 0));
        check("negative defense treated as 0",
                c.applyDefense(100, -50) == 100.0, c.applyDefense(100, -50));
        CombatSettings weird = new CombatSettings(0.0, 0.0, true);
        check("defenseConstant<1 clamped to 1",
                weird.applyDefense(10, 0) == 10.0, weird.applyDefense(10, 0));

        System.out.println();
        System.out.println("[2] ResetSettings.costFor (amount=50, scaling=1.5)");
        ResetSettings r = ResetSettings.defaults();
        check("0회 -> 50",  r.costFor(0) == 50,  r.costFor(0));
        check("1회 -> 75",  r.costFor(1) == 75,  r.costFor(1));
        check("2회 -> 113 (112.5 올림)", r.costFor(2) == 113, r.costFor(2));
        check("음수 횟수는 0으로", r.costFor(-3) == 50, r.costFor(-3));
        ResetSettings noScale = new ResetSettings(true, "x", 50, 0.5);
        check("scaling<1 은 1로 취급", noScale.costFor(5) == 50, noScale.costFor(5));

        System.out.println();
        System.out.println("[3] DerivedStats");
        DerivedStats d = DerivedStats.builder()
                .set(DerivedStat.MAX_HEALTH, 100)
                .add(DerivedStat.MAX_HEALTH, 30)
                .add(DerivedStat.DEFENSE, 0)
                .build();
        check("set 후 add 누적", d.get(DerivedStat.MAX_HEALTH) == 130.0, d.get(DerivedStat.MAX_HEALTH));
        check("없는 값은 0", d.get(DerivedStat.CRIT_CHANCE) == 0.0, d.get(DerivedStat.CRIT_CHANCE));
        check("empty 는 전부 0", DerivedStats.empty().get(DerivedStat.MAX_HEALTH) == 0.0, "");
        check("configKey 역매핑",
                DerivedStat.fromConfigKey("critChance") == DerivedStat.CRIT_CHANCE,
                DerivedStat.fromConfigKey("critChance"));
        check("모르는 configKey 는 null", DerivedStat.fromConfigKey("nope") == null, "");

        System.out.println();
        System.out.println("[4] StatType");
        StatType st = new StatType("strength", "힘",
                Map.of(DerivedStat.PHYSICAL_DAMAGE, 2.0), 0);
        check("perPoint", st.perPoint(DerivedStat.PHYSICAL_DAMAGE) == 2.0, "");
        check("없는 파생은 0", st.perPoint(DerivedStat.DEFENSE) == 0.0, "");
        try {
            st.perPoint().put(DerivedStat.DEFENSE, 1.0);
            check("perPoint 불변", false, "수정됨");
        } catch (UnsupportedOperationException e) {
            check("perPoint 불변", true, "");
        }

        System.out.println();
        System.out.println("[5] GuiScreen / UiSettings");
        check("rows 0 -> 1", new GuiScreen("x", "t", 0, Map.of()).rows() == 1, "");
        check("rows 9 -> 6", new GuiScreen("x", "t", 9, Map.of()).rows() == 6, "");
        check("size = rows*9", new GuiScreen("x", "t", 3, Map.of()).size() == 27, "");
        GuiScreen screen = new GuiScreen("stat", "스탯", 3,
                Map.of("info", new GuiIcon(4, "PAPER")));
        check("icon 조회", screen.icon("info").slot() == 4, "");
        check("없는 icon 은 null", screen.icon("nope") == null, "");
        check("fallback 화면", GuiScreen.fallback("x").icons().isEmpty(), "");
        check("ui 주기 0 -> 1", new UiSettings(0, Set.of()).updateIntervalTicks() == 1, "");

        System.out.println();
        System.out.println("[6] StatSettings");
        StatSettings s = new StatSettings(
                new LinkedHashMap<>(Map.of("strength", st)),
                new EnumMap<>(Map.of(DerivedStat.MAX_HEALTH, 100.0)),
                ResetSettings.defaults());
        check("stat 조회", s.stat("strength") == st, "");
        check("없는 stat 은 null", s.stat("nope") == null, "");
        check("base 조회", s.base(DerivedStat.MAX_HEALTH) == 100.0, "");
        check("없는 base 는 0", s.base(DerivedStat.MAX_MANA) == 0.0, "");
        check("defaults 는 능력치 없음", StatSettings.defaults().stats().isEmpty(), "");

        System.out.println();
        System.out.println("결과: " + pass + " 통과 / " + fail + " 실패");
        if (fail > 0) System.exit(1);
    }
}
