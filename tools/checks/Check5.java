import com.example.rpgcore.quest.*;
import com.example.rpgcore.quest.objective.*;
import com.example.rpgcore.quest.repeat.ResetCycle;
import com.example.rpgcore.quest.reward.QuestReward;
import com.example.rpgcore.region.RegionDefinition;
import java.util.*;

public class Check5 {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok, Object got) {
        if (ok) { pass++; System.out.println("  PASS  " + n); }
        else { fail++; System.out.println("  FAIL  " + n + "  (got: " + got + ")"); }
    }

    public static void main(String[] a) {
        System.out.println("[1] Objective / ObjectiveType");
        Objective kill = new Objective(ObjectiveType.KILL, "ZOMBIE", 10);
        check("미완료", !kill.isComplete(9), "");
        check("정확히 도달하면 완료", kill.isComplete(10), "");
        check("넘어도 완료", kill.isComplete(50), "");
        check("amount 0 은 1로", new Objective(ObjectiveType.REACH, "x", 0).amount() == 1, "");
        check("키 필드", ObjectiveType.KILL.keyField().equals("target"), "");
        check("REACH 키 필드", ObjectiveType.REACH.keyField().equals("region"), "");
        check("이름 파싱", ObjectiveType.fromConfig("collect") == ObjectiveType.COLLECT, "");
        check("모르는 이름은 null", ObjectiveType.fromConfig("nope") == null, "");

        System.out.println();
        System.out.println("[2] QuestType — 기획서 7장");
        check("DAILY 는 주기형", QuestType.DAILY.cyclic(), "");
        check("WEEKLY 는 주기형", QuestType.WEEKLY.cyclic(), "");
        check("NORMAL 은 아님", !QuestType.NORMAL.cyclic(), "");
        check("MAIN 은 아님", !QuestType.MAIN.cyclic(), "");

        System.out.println();
        System.out.println("[3] QuestDefinition");
        QuestDefinition normal = new QuestDefinition("q1", "q1", QuestType.NORMAL, 5, null,
                List.of(kill), QuestReward.none(), false);
        QuestDefinition daily = new QuestDefinition("q2", "q2", QuestType.DAILY, 1, null,
                List.of(kill), QuestReward.none(), false);
        QuestDefinition repeat = new QuestDefinition("q3", "q3", QuestType.NORMAL, 1, null,
                List.of(kill), QuestReward.none(), true);
        check("일반 1회성은 반복 불가", !normal.canRepeat(), "");
        check("DAILY 는 반복 가능", daily.canRepeat(), "");
        check("repeatable 이면 반복 가능", repeat.canRepeat(), "");
        check("보상 없음 판정", QuestReward.none().isEmpty(), "");
        check("보상 있음 판정",
                !new QuestReward(100, 0, 0, Map.of()).isEmpty(), "");

        System.out.println();
        System.out.println("[4] QuestProgress — 저장 왕복");
        QuestProgress p = new QuestProgress(1000L, 3);
        check("초기값 0", p.count(0) == 0 && p.count(2) == 0, "");
        check("범위 밖은 0", p.count(9) == 0, "");
        check("advance", p.advance(0, 4) && p.count(0) == 4, p.count(0));
        check("범위 밖 advance 는 무시", !p.advance(9, 1), "");
        check("0 이하 advance 는 무시", !p.advance(0, 0), "");
        Map<String, Object> raw = p.toMap();
        QuestProgress back = QuestProgress.fromMap(raw, 3);
        check("왕복 후 값 동일", back.count(0) == 4, back.count(0));
        check("왕복 후 시작 시각 동일", back.startedAt() == 1000L, back.startedAt());

        // 정의를 고쳐 목표가 늘거나 줄어도 앞에서부터 옮겨진다
        QuestProgress grown = QuestProgress.fromMap(raw, 5);
        check("목표가 늘면 앞값 유지", grown.count(0) == 4 && grown.size() == 5, grown.size());
        QuestProgress shrunk = QuestProgress.fromMap(raw, 1);
        check("목표가 줄면 잘림", shrunk.count(0) == 4 && shrunk.size() == 1, shrunk.size());
        check("null 이면 전부 0", QuestProgress.fromMap(null, 2).count(0) == 0, "");

        System.out.println();
        System.out.println("[5] ResetCycle — 기간 판정");
        long day = 24L * 60 * 60 * 1000;
        check("한 번도 안 했으면 리셋 대상", ResetCycle.DAILY.isDue(0, 1000), "");
        check("하루 안 지났으면 아님", !ResetCycle.DAILY.isDue(1000, 1000 + day - 1), "");
        check("하루 지나면 대상", ResetCycle.DAILY.isDue(1000, 1000 + day), "");
        check("주간은 7일", ResetCycle.WEEKLY.periodMillis() == 7 * day, "");
        check("남은 시간", ResetCycle.DAILY.remaining(1000, 1000 + day / 2) == day / 2, "");
        check("지났으면 남은 시간 0", ResetCycle.DAILY.remaining(1000, 1000 + day * 2) == 0, "");

        System.out.println();
        System.out.println("[6] RegionDefinition — 좌표 판정");
        RegionDefinition r = RegionDefinition.of("start_field", "시작 평원", "world",
                500, 500, 0, 0, 10, 1);
        check("두 점이 뒤집혀도 정리됨", r.minX() == 0 && r.maxX() == 500, r.minX() + ".." + r.maxX());
        check("레벨대도 정리됨", r.minLevel() == 1 && r.maxLevel() == 10, "");
        check("안쪽", r.contains("world", 250, 250), "");
        check("경계선 포함", r.contains("world", 0, 500), "");
        check("바깥", !r.contains("world", 501, 250), "");
        check("다른 월드", !r.contains("nether", 250, 250), "");
        check("넓이", r.area() == 500 * 500.0, r.area());

        System.out.println();
        System.out.println("결과: " + pass + " 통과 / " + fail + " 실패");
        if (fail > 0) System.exit(1);
    }
}
