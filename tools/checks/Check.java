import com.example.rpgcore.binding.HoldState;
import com.example.rpgcore.binding.InputTrigger;
import com.example.rpgcore.config.schema.CurveSettings;
import com.example.rpgcore.config.validation.ValidationReport;
import com.example.rpgcore.level.ExpCurve;
import com.example.rpgcore.life.TrackType;
import com.example.rpgcore.player.data.PlayerData;
import com.example.rpgcore.player.data.PlayerDataCodec;
import java.util.*;

public class Check {
    static int pass = 0, fail = 0;

    static void check(String name, boolean ok, Object got) {
        if (ok) { pass++; System.out.println("  PASS  " + name); }
        else { fail++; System.out.println("  FAIL  " + name + "  (got: " + got + ")"); }
    }

    static Map<String,Object> map(Object... kv) {
        Map<String,Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put((String) kv[i], kv[i+1]);
        return m;
    }

    public static void main(String[] args) {
        System.out.println("[1] ExpCurve (levels.yml base=100 factor=1.12)");
        ExpCurve curve = ExpCurve.from(new CurveSettings("EXPONENTIAL", 100, 1.12),
                CurveSettings.defaultCombat(), "levels.yml", "combat.curve", null);
        check("required(1) == 100", Math.abs(curve.requiredExp(1) - 100.0) < 1e-9, curve.requiredExp(1));
        check("required(2) == 112", Math.abs(curve.requiredExp(2) - 112.0) < 1e-9, curve.requiredExp(2));
        check("required(0) treated as 1", Math.abs(curve.requiredExp(0) - 100.0) < 1e-9, curve.requiredExp(0));
        check("required(1000) exceeds long range",
                curve.requiredExp(1000) > (double) Long.MAX_VALUE, curve.requiredExp(1000));

        ValidationReport rep = new ValidationReport();
        ExpCurve bad = ExpCurve.from(new CurveSettings("LINEAR", 100, 1.12),
                CurveSettings.defaultCombat(), "levels.yml", "combat.curve", rep);
        check("unknown curve type -> error logged, not thrown", rep.errorCount() == 1, rep.entries());
        check("unknown curve type -> falls back to EXPONENTIAL",
                bad.type().equals("EXPONENTIAL"), bad.type());

        ValidationReport rep2 = new ValidationReport();
        ExpCurve zero = ExpCurve.from(new CurveSettings("EXPONENTIAL", 0, 1.12),
                CurveSettings.defaultCombat(), "levels.yml", "combat.curve", rep2);
        check("base<=0 -> falls back", zero.requiredExp(1) == 100.0 && rep2.errorCount() == 1,
                zero.requiredExp(1));

        System.out.println();
        System.out.println("[2] PlayerDataCodec round trip + 알 수 없는 키 보존 (지시서 7장)");
        UUID id = UUID.fromString("11111111-2222-3333-4444-555555555555");
        Map<String,Object> raw = map(
            "uuid", id.toString(),
            "name", "tester",
            "lastLogin", 1234567890123L,
            "combat", map("level", 7, "exp", 250.5, "statPoints", 30,
                          "stats", map("strength", 4, "vitality", 2),
                          "statResetCount", 1,
                          "futureField", "보존되어야 함"),
            "life", map("living", map("level", 3, "exp", 10.0),
                        "mining", map("level", 2, "exp", 5.0),
                        "unlocked", new ArrayList<>(List.of("recipe_example"))),
            "job", map("base", "swordsman", "tier1", null, "tier2", null),
            "skill", map("points", 6,
                         "unlocked", new ArrayList<>(List.of("example_slash", "no_level_entry")),
                         "levels", map("example_slash", 12)),
            "binding", map("itemSlots", new ArrayList<>(Arrays.asList("example_slash", null)),
                           "keyCombos", new ArrayList<>(List.of(
                               map("hold", "SNEAK", "trigger", "RIGHT_CLICK", "skill", "example_slash"),
                               map("hold", "CRAWL", "trigger", "RIGHT_CLICK", "skill", "bogus")))),
            "quest", map("active", map("example_quest", map("kill_zombie", 3)),
                         "completed", new ArrayList<>(List.of("intro")),
                         "dailyResetAt", 100L, "weeklyResetAt", 200L),
            "currency", map("dungeon_coin", 42),
            "someFutureSection", map("a", 1)
        );

        ValidationReport r = new ValidationReport();
        PlayerData data = PlayerDataCodec.fromMap(id, raw, r);

        check("combat.level", data.combat().level() == 7, data.combat().level());
        check("combat.exp", data.combat().exp() == 250.5, data.combat().exp());
        check("combat.stats.strength", data.combat().stat("strength") == 4, data.combat().stat("strength"));
        check("life.living.level", data.life().track(TrackType.LIVING).level() == 3,
                data.life().track(TrackType.LIVING).level());
        check("life.crafting defaults to 1", data.life().track(TrackType.CRAFTING).level() == 1,
                data.life().track(TrackType.CRAFTING).level());
        check("job.base", "swordsman".equals(data.job().base()), data.job().base());
        check("job.stage == 1 (기본만 선택)", data.job().stage() == 1, data.job().stage());
        check("skill.levelOf 등록된 스킬", data.skill().levelOf("example_slash") == 12,
                data.skill().levelOf("example_slash"));
        check("skill.levelOf 해금했지만 levels 없음 -> 1", data.skill().levelOf("no_level_entry") == 1,
                data.skill().levelOf("no_level_entry"));
        check("skill.levelOf 미해금 -> 0", data.skill().levelOf("nope") == 0,
                data.skill().levelOf("nope"));
        check("binding.itemSlots 빈 칸은 null 유지",
                data.binding().itemSlots().size() == 2 && data.binding().itemSlots().get(1) == null,
                data.binding().itemSlots());
        check("keyCombos: 잘못된 hold 값은 건너뜀", data.binding().keyCombos().size() == 1,
                data.binding().keyCombos());
        check("keyCombos: 건너뛴 항목이 리포트에 남음", r.size() == 1, r.entries());
        PlayerData.KeyCombo combo = data.binding().keyCombos().get(0);
        check("keyCombo 값", combo.hold() == HoldState.SNEAK
                && combo.trigger() == InputTrigger.RIGHT_CLICK
                && combo.skillId().equals("example_slash"), combo);
        check("currency", data.currency("dungeon_coin") == 42L, data.currency("dungeon_coin"));
        check("quest.active 보존", data.quest().active().containsKey("example_quest"),
                data.quest().active());

        check("알 수 없는 최상위 섹션 보존", data.unknown().containsKey("someFutureSection"),
                data.unknown().keySet());
        Object combatUnknown = data.unknown().get("combat");
        check("알 수 없는 하위 키 보존 (combat.futureField)",
                combatUnknown instanceof Map<?,?> m && "보존되어야 함".equals(m.get("futureField")),
                combatUnknown);
        check("해석된 키는 unknown 에 남지 않음",
                combatUnknown instanceof Map<?,?> m && m.size() == 1, combatUnknown);
        check("빈 껍데기 섹션은 정리됨 (job/skill/binding/quest/currency 없음)",
                !data.unknown().containsKey("job") && !data.unknown().containsKey("skill")
                && !data.unknown().containsKey("binding") && !data.unknown().containsKey("quest")
                && !data.unknown().containsKey("currency"), data.unknown().keySet());

        System.out.println();
        System.out.println("[3] toMap -> fromMap 왕복");
        Map<String,Object> out = PlayerDataCodec.toMap(data);
        check("toMap 이 알 수 없는 키를 다시 씀",
                out.containsKey("someFutureSection")
                && ((Map<?,?>) out.get("combat")).get("futureField") != null, out.keySet());
        PlayerData again = PlayerDataCodec.fromMap(id, out, null);
        check("왕복 후 level 동일", again.combat().level() == 7, again.combat().level());
        check("왕복 후 exp 동일", again.combat().exp() == 250.5, again.combat().exp());
        check("왕복 후 스킬 레벨 동일", again.skill().levelOf("example_slash") == 12,
                again.skill().levelOf("example_slash"));
        check("왕복 후 keyCombo 동일", again.binding().keyCombos().size() == 1,
                again.binding().keyCombos());
        check("왕복 후 알 수 없는 키 동일",
                ((Map<?,?>) again.unknown().get("combat")).get("futureField") != null,
                again.unknown());
        check("왕복 후 unknown 이 불어나지 않음", again.unknown().size() == data.unknown().size(),
                again.unknown().keySet());

        System.out.println();
        System.out.println("[4] 신규 플레이어 기본값");
        PlayerData fresh = PlayerDataCodec.fromMap(id, null, null);
        check("level 1", fresh.combat().level() == 1, fresh.combat().level());
        check("exp 0", fresh.combat().exp() == 0.0, fresh.combat().exp());
        check("job 미선택", !fresh.job().hasBase() && fresh.job().stage() == 0, fresh.job().stage());
        check("dirty 아님", !fresh.isDirty(), fresh.isDirty());

        System.out.println();
        System.out.println("[5] 잘못된 값이 서버를 죽이지 않는다 (지시서 6장)");
        ValidationReport r3 = new ValidationReport();
        PlayerData broken = PlayerDataCodec.fromMap(id,
                map("combat", map("level", "일곱", "exp", "많음"),
                    "lastLogin", "어제"), r3);
        check("문자열 level -> 기본 1", broken.combat().level() == 1, broken.combat().level());
        check("문자열 exp -> 기본 0", broken.combat().exp() == 0.0, broken.combat().exp());
        check("문제 3건이 리포트에 남음", r3.size() == 3, r3.entries());

        ValidationReport r4 = new ValidationReport();
        PlayerData negative = PlayerDataCodec.fromMap(id, map("combat", map("level", -5)), r4);
        check("음수 level -> 1로 교정", negative.combat().level() == 1, negative.combat().level());

        System.out.println();
        System.out.println("결과: " + pass + " 통과 / " + fail + " 실패");
        if (fail > 0) System.exit(1);
    }
}
