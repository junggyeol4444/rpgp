import com.example.rpgcore.config.schema.SkillSettings;
import com.example.rpgcore.skill.*;
import com.example.rpgcore.skill.effect.*;
import java.util.*;

public class Check4 {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok, Object got) {
        if (ok) { pass++; System.out.println("  PASS  " + n); }
        else { fail++; System.out.println("  FAIL  " + n + "  (got: " + got + ")"); }
    }

    static SkillDefinition skill(String id, String job, String parent, String branch) {
        return skill(id, job, SkillStage.BASE, null, parent, branch);
    }

    static SkillDefinition skill(String id, String job, SkillStage stage,
                                 String requireBranch, String parent, String branch) {
        return new SkillDefinition(id, id, job, stage, requireBranch, parent, branch,
                20, 0.5, 6.0, 0.0, SkillDefinition.DEFAULT_MAX_LEVEL,
                new PowerScaling(30, PowerScaling.DIMINISHING, 4.0, 0.5),
                Map.of("strength", 1.2),
                List.of(new SkillEffect(EffectType.DAMAGE_CONE,
                        Map.of("range", 4.0, "angle", 90.0))));
    }

    public static void main(String[] a) {
        System.out.println("[1] PowerScaling — 기획서 6장 고레벨 감쇠");
        PowerScaling p = new PowerScaling(30, PowerScaling.DIMINISHING, 4.0, 0.5);
        check("1레벨 = base + 4*1 = 34", p.powerAt(1) == 34.0, p.powerAt(1));
        check("4레벨 = 30 + 4*2 = 38", p.powerAt(4) == 38.0, p.powerAt(4));
        check("100레벨 = 30 + 40 = 70", p.powerAt(100) == 70.0, p.powerAt(100));
        check("0레벨은 1레벨로", p.powerAt(0) == p.powerAt(1), p.powerAt(0));
        double d1 = p.powerAt(2) - p.powerAt(1);
        double d2 = p.powerAt(101) - p.powerAt(100);
        check("고레벨 상승폭이 더 완만", d2 < d1, d1 + " vs " + d2);
        check("9999레벨도 유한", Double.isFinite(p.powerAt(9999)), p.powerAt(9999));

        System.out.println();
        System.out.println("[2] SkillDefinition — 마나·쿨타임");
        SkillDefinition s = skill("swordsman_strike", "swordsman", null, null);
        check("1레벨 마나 20", s.manaAt(1) == 20.0, s.manaAt(1));
        check("11레벨 마나 25", s.manaAt(11) == 25.0, s.manaAt(11));
        check("쿨타임 감소 0이면 그대로", s.cooldownAt(50) == 6.0, s.cooldownAt(50));
        SkillDefinition fast = new SkillDefinition("x", "x", "j", SkillStage.BASE, null, null, null,
                0, 0, 6.0, 1.0, 9999, PowerScaling.defaults(), Map.of(), List.of());
        check("쿨타임은 0 밑으로 안 감", fast.cooldownAt(100) == 0.0, fast.cooldownAt(100));
        check("선행 없음", !s.hasParent(), "");
        check("분기 없음", !s.inBranchGroup(), "");
        check("maxLevel 기본 9999",
                s.maxLevel() == SkillDefinition.DEFAULT_MAX_LEVEL, s.maxLevel());

        System.out.println();
        System.out.println("[3] SkillStage — 전직 단계 요구");
        check("BASE 는 직업 단계 1", SkillStage.BASE.requiredJobStage() == 1, "");
        check("TIER1 은 2", SkillStage.TIER1.requiredJobStage() == 2, "");
        check("TIER2 는 3", SkillStage.TIER2.requiredJobStage() == 3, "");
        check("이름 파싱", SkillStage.fromConfig("tier1") == SkillStage.TIER1, "");
        check("모르는 이름은 null", SkillStage.fromConfig("nope") == null, "");

        System.out.println();
        System.out.println("[3b] 전직 분기 조건 (8단계)");
        SkillDefinition baseSkill = skill("b", "swordsman", null, null);
        SkillDefinition tier1Skill =
                skill("t", "swordsman", SkillStage.TIER1, "guardian", null, null);
        check("기본 스킬은 분기 조건 없음", !baseSkill.hasBranchRequirement(), "");
        check("1차 스킬은 분기 조건 있음", tier1Skill.hasBranchRequirement(), "");
        check("분기 id", "guardian".equals(tier1Skill.requireBranch()), "");
        check("1차 스킬은 직업 단계 2 필요",
                tier1Skill.stage().requiredJobStage() == 2, "");

        System.out.println();
        System.out.println("[4] SkillTree — 분기 그룹과 선행 (지시서 8장 [규칙])");
        Map<String, SkillDefinition> map = new LinkedHashMap<>();
        map.put("root",  skill("root", "swordsman", null, null));
        map.put("left",  skill("left", "swordsman", "root", "stance"));
        map.put("right", skill("right", "swordsman", "root", "stance"));
        map.put("other", skill("other", "archer", null, null));
        SkillTree tree = new SkillTree(map);

        check("get", tree.get("left") == map.get("left"), "");
        check("없는 id 는 null", tree.get("nope") == null, "");
        check("size", tree.size() == 4, tree.size());
        check("직업별 목록", tree.ofJob("swordsman").size() == 3, tree.ofJob("swordsman").size());
        check("없는 직업은 빈 목록", tree.ofJob("nope").isEmpty(), "");
        check("분기 그룹 2개", tree.branchGroup("stance").size() == 2, "");
        check("형제는 자신을 뺀 나머지",
                tree.siblingsOf(map.get("left")).equals(Set.of("right")),
                tree.siblingsOf(map.get("left")));
        check("분기 없으면 형제 없음", tree.siblingsOf(map.get("root")).isEmpty(), "");
        check("자식 조회", tree.childrenOf("root").size() == 2, tree.childrenOf("root").size());
        check("empty 트리", SkillTree.empty().size() == 0, "");

        System.out.println();
        System.out.println("[5] SkillSettings.slotCount — 기획서 6장 (시작 2, 전직마다 +1)");
        SkillSettings st = SkillSettings.defaults();
        check("직업 미선택 0단계 -> 2칸", st.slotCount(0) == 2, st.slotCount(0));
        check("기본 직업 1단계 -> 2칸", st.slotCount(1) == 2, st.slotCount(1));
        check("1차 전직 2단계 -> 3칸", st.slotCount(2) == 3, st.slotCount(2));
        check("2차 전직 3단계 -> 4칸", st.slotCount(3) == 4, st.slotCount(3));
        check("해금 비용 기본 0", st.unlockCost() == 0, st.unlockCost());

        System.out.println();
        System.out.println("[6] SkillEffect");
        SkillEffect e = new SkillEffect(EffectType.DAMAGE_CONE,
                Map.of("range", 4.0, "angle", 90.0));
        check("값 조회", e.value("range", 1.0) == 4.0, "");
        check("없는 값은 기본", e.value("nope", 7.0) == 7.0, "");
        check("타입 파싱", EffectType.fromConfig("heal_self") == EffectType.HEAL_SELF, "");
        check("모르는 타입은 null", EffectType.fromConfig("nope") == null, "");

        System.out.println();
        System.out.println("결과: " + pass + " 통과 / " + fail + " 실패");
        if (fail > 0) System.exit(1);
    }
}
