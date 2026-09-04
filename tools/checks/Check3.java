import com.example.rpgcore.config.schema.JobSettings;
import com.example.rpgcore.job.*;
import java.util.*;

public class Check3 {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok, Object got) {
        if (ok) { pass++; System.out.println("  PASS  " + n); }
        else { fail++; System.out.println("  FAIL  " + n + "  (got: " + got + ")"); }
    }

    public static void main(String[] a) {
        System.out.println("[1] JobTree — 기획서 5장 구조 (기본 1 -> 1차 2 -> 2차 2)");
        JobBranch counter = new JobBranch("counter", "반격 특화", Map.of(), Map.of());
        JobBranch protect = new JobBranch("protect", "수호", Map.of(), Map.of());
        Map<String, JobBranch> guardianChildren = new LinkedHashMap<>();
        guardianChildren.put("counter", counter);
        guardianChildren.put("protect", protect);
        JobBranch guardian = new JobBranch("guardian", "방어형",
                Map.of("vitality", 3), guardianChildren);
        JobBranch swift = new JobBranch("swift", "속공형", Map.of("agility", 3), Map.of());

        Map<String, JobBranch> tier1 = new LinkedHashMap<>();
        tier1.put("guardian", guardian);
        tier1.put("swift", swift);
        JobDefinition sword = new JobDefinition("swordsman", "근접 검", "MELEE_SWORD",
                Map.of("strength", 2, "vitality", 1), tier1, 0);
        Map<String, JobDefinition> base = new LinkedHashMap<>();
        base.put("swordsman", sword);
        JobTree tree = new JobTree(base);

        check("base 조회", tree.base("swordsman") == sword, "");
        check("없는 base 는 null", tree.base("nope") == null, "");
        check("null id 는 null", tree.base(null) == null, "");
        check("hasBase", tree.hasBase("swordsman") && !tree.hasBase("nope"), "");
        check("size", tree.size() == 1, tree.size());
        check("tier1 조회", tree.tier1("swordsman", "guardian") == guardian, "");
        check("없는 tier1 은 null", tree.tier1("swordsman", "nope") == null, "");
        check("tier2 조회", tree.tier2("swordsman", "guardian", "counter") == counter, "");
        check("없는 경로의 tier2 는 null",
                tree.tier2("swordsman", "swift", "counter") == null, "");
        check("empty 트리", JobTree.empty().size() == 0, "");
        check("baseJobs 순서 유지",
                new ArrayList<>(tree.baseJobs()).get(0) == sword, "");

        System.out.println();
        System.out.println("[2] JobDefinition");
        check("보정치 조회", sword.statBonusPerLevel("strength") == 2, "");
        check("없는 능력치는 0", sword.statBonusPerLevel("agility") == 0, "");
        try {
            sword.statBonusPerLevel().put("agility", 5);
            check("보정치 맵 불변", false, "수정됨");
        } catch (UnsupportedOperationException e) {
            check("보정치 맵 불변", true, "");
        }
        // 생성 후 원본 맵을 고쳐도 트리에 영향이 없어야 한다
        tier1.put("hacked", swift);
        check("tier1 은 복사본", sword.tier1().size() == 2, sword.tier1().keySet());
        base.put("hacked", sword);
        check("baseJobs 는 복사본", tree.size() == 1, tree.size());

        System.out.println();
        System.out.println("[2b] 1차 분기 스탯 보정 (8단계)");
        check("분기 보정 조회", guardian.statBonusPerLevel("vitality") == 3, "");
        check("없는 능력치는 0", guardian.statBonusPerLevel("strength") == 0, "");
        check("보정 없는 분기는 0", counter.statBonusPerLevel("vitality") == 0, "");
        try {
            guardian.statBonusPerLevel().put("strength", 1);
            check("분기 보정 맵 불변", false, "수정됨");
        } catch (UnsupportedOperationException e) {
            check("분기 보정 맵 불변", true, "");
        }

        System.out.println();
        System.out.println("[3] JobSettings 기본값 (기획서 5장 시점)");
        JobSettings s = JobSettings.defaults();
        check("기본 직업 선택 3레벨", s.jobSelectLevel() == 3, s.jobSelectLevel());
        check("1차 전직 20레벨", s.tier1Level() == 20, s.tier1Level());
        check("2차 전직 50레벨", s.tier2Level() == 50, s.tier2Level());
        check("분기 되돌리기 불가", !s.branchRevert(), s.branchRevert());
        check("전직 퀘스트 id", s.tier1Quest().equals("job_advance_1"), s.tier1Quest());

        System.out.println();
        System.out.println("결과: " + pass + " 통과 / " + fail + " 실패");
        if (fail > 0) System.exit(1);
    }
}
