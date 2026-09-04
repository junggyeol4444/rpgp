import com.example.rpgcore.quest.QuestDefinition;
import com.example.rpgcore.quest.QuestType;
import com.example.rpgcore.quest.editor.QuestDraft;
import com.example.rpgcore.quest.objective.Objective;
import com.example.rpgcore.quest.objective.ObjectiveType;
import com.example.rpgcore.quest.reward.QuestReward;
import java.util.List;
import java.util.Map;

/** 10단계 — 퀘스트 GUI 에디터의 초안이 값을 잃지 않는지 본다. */
public class Check7 {
    static int pass = 0, fail = 0;

    static void check(String name, boolean ok, Object got) {
        if (ok) { pass++; System.out.println("  PASS  " + name); }
        else { fail++; System.out.println("  FAIL  " + name + "  (got: " + got + ")"); }
    }

    public static void main(String[] args) {
        System.out.println("[1] QuestDraft 기본값");
        QuestDraft fresh = new QuestDraft("my_quest");
        check("id 그대로", "my_quest".equals(fresh.id()), fresh.id());
        check("표시 이름 기본은 id", "my_quest".equals(fresh.display()), fresh.display());
        check("종류 기본 NORMAL", fresh.type() == QuestType.NORMAL, fresh.type());
        check("필요 레벨 기본 1", fresh.requireLevel() == 1, fresh.requireLevel());
        check("필요 직업 기본 없음", fresh.requireJob() == null, fresh.requireJob());
        check("반복 기본 꺼짐", !fresh.repeatable(), fresh.repeatable());
        check("목표 없으면 저장 불가", !fresh.isValid(), fresh.isValid());

        System.out.println("[2] 값 넣기");
        fresh.display("");
        check("빈 표시 이름은 id 로 되돌림", "my_quest".equals(fresh.display()), fresh.display());
        fresh.display("내 퀘스트");
        check("표시 이름 반영", "내 퀘스트".equals(fresh.display()), fresh.display());
        fresh.requireLevel(-5);
        check("필요 레벨 하한 1", fresh.requireLevel() == 1, fresh.requireLevel());
        fresh.requireLevel(12);
        check("필요 레벨 반영", fresh.requireLevel() == 12, fresh.requireLevel());
        fresh.toggleRepeatable();
        check("반복 토글", fresh.repeatable(), fresh.repeatable());
        fresh.addSkillPoints(-3);
        check("스킬 포인트 하한 0", fresh.skillPoints() == 0, fresh.skillPoints());
        fresh.addSkillPoints(4);
        fresh.addSkillPoints(-1);
        check("스킬 포인트 누적", fresh.skillPoints() == 3, fresh.skillPoints());
        fresh.addStatPoints(2);
        check("스탯 포인트 누적", fresh.statPoints() == 2, fresh.statPoints());
        fresh.addCombatExp(-100);
        check("경험치 하한 0", fresh.combatExp() == 0, fresh.combatExp());
        fresh.addCombatExp(250);
        check("경험치 누적", Math.abs(fresh.combatExp() - 250) < 1e-9, fresh.combatExp());

        System.out.println("[3] 종류 순환");
        QuestDraft cycle = new QuestDraft("c");
        QuestType[] types = QuestType.values();
        boolean allSeen = true;
        for (int i = 1; i <= types.length; i++) {
            cycle.cycleType();
            if (cycle.type() != types[i % types.length]) { allSeen = false; }
        }
        check("한 바퀴 돌면 제자리", allSeen && cycle.type() == QuestType.NORMAL, cycle.type());

        System.out.println("[4] 목표 다루기");
        QuestDraft obj = new QuestDraft("o");
        obj.addObjective();
        check("추가하면 하나", obj.objectives().size() == 1, obj.objectives().size());
        check("추가하면 저장 가능", obj.isValid(), obj.isValid());
        check("기본 목표는 KILL",
                obj.objectives().get(0).type() == ObjectiveType.KILL,
                obj.objectives().get(0).type());
        obj.setObjectiveTarget(0, "SKELETON", 7);
        check("대상 반영", "SKELETON".equals(obj.objectives().get(0).key()),
                obj.objectives().get(0).key());
        check("개수 반영", obj.objectives().get(0).amount() == 7,
                obj.objectives().get(0).amount());
        obj.cycleObjectiveType(0);
        check("종류만 바뀌고 대상은 남음",
                obj.objectives().get(0).type() == ObjectiveType.COLLECT
                        && "SKELETON".equals(obj.objectives().get(0).key())
                        && obj.objectives().get(0).amount() == 7,
                obj.objectives().get(0));
        obj.setObjectiveTarget(9, "X", 1);
        check("범위 밖 대상 지정은 무시", obj.objectives().size() == 1, obj.objectives().size());
        obj.cycleObjectiveType(-1);
        check("범위 밖 순환은 무시",
                obj.objectives().get(0).type() == ObjectiveType.COLLECT,
                obj.objectives().get(0).type());
        obj.removeObjective(5);
        check("범위 밖 삭제는 무시", obj.objectives().size() == 1, obj.objectives().size());
        obj.removeObjective(0);
        check("삭제 반영", obj.objectives().isEmpty(), obj.objectives().size());
        check("다 지우면 저장 불가", !obj.isValid(), obj.isValid());

        System.out.println("[5] 정의 -> 초안 -> 정의 왕복");
        QuestDefinition source = new QuestDefinition("round_trip", "왕복", QuestType.WEEKLY, 30,
                "archer",
                List.of(new Objective(ObjectiveType.COLLECT, "IRON_INGOT", 12),
                        new Objective(ObjectiveType.TALK, "elder", 1)),
                new QuestReward(1500.0, 2, 1, Map.of("gold", 40L)),
                true);
        QuestDefinition back = QuestDraft.from(source).toDefinition();
        check("id", source.id().equals(back.id()), back.id());
        check("표시 이름", source.display().equals(back.display()), back.display());
        check("종류", source.type() == back.type(), back.type());
        check("필요 레벨", source.requireLevel() == back.requireLevel(), back.requireLevel());
        check("필요 직업", source.requireJob().equals(back.requireJob()), back.requireJob());
        check("목표 그대로", source.objectives().equals(back.objectives()), back.objectives());
        check("보상 그대로", source.reward().equals(back.reward()), back.reward());
        check("반복 여부", source.repeatable() == back.repeatable(), back.repeatable());

        System.out.println("[6] 초안은 원본을 건드리지 않는다");
        QuestDraft copy = QuestDraft.from(source);
        copy.addObjective();
        copy.display("바뀐 이름");
        check("원본 목표 개수 그대로", source.objectives().size() == 2, source.objectives().size());
        check("원본 표시 이름 그대로", "왕복".equals(source.display()), source.display());

        System.out.println("[7] 하한 보정은 저장할 때도 걸린다");
        QuestDraft floor = new QuestDraft("floor");
        floor.addObjective();
        floor.setObjectiveTarget(0, "ZOMBIE", -4);
        check("목표 개수 하한 1", floor.objectives().get(0).amount() == 1,
                floor.objectives().get(0).amount());
        check("필요 레벨 하한 1", floor.toDefinition().requireLevel() == 1,
                floor.toDefinition().requireLevel());

        System.out.println("[8] 목표 종류마다 설정 키 이름이 다르다");
        check("KILL 은 target", "target".equals(ObjectiveType.KILL.keyField()),
                ObjectiveType.KILL.keyField());
        check("COLLECT 는 item", "item".equals(ObjectiveType.COLLECT.keyField()),
                ObjectiveType.COLLECT.keyField());
        check("REACH 는 region", "region".equals(ObjectiveType.REACH.keyField()),
                ObjectiveType.REACH.keyField());
        check("TALK 은 npc", "npc".equals(ObjectiveType.TALK.keyField()),
                ObjectiveType.TALK.keyField());

        System.out.println();
        System.out.println("결과: " + pass + " 통과 / " + fail + " 실패");
        if (fail > 0) { System.exit(1); }
    }
}
