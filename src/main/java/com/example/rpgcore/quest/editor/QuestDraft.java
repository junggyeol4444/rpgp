package com.example.rpgcore.quest.editor;

import com.example.rpgcore.quest.QuestDefinition;
import com.example.rpgcore.quest.QuestType;
import com.example.rpgcore.quest.objective.Objective;
import com.example.rpgcore.quest.objective.ObjectiveType;
import com.example.rpgcore.quest.reward.QuestReward;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 편집 중인 퀘스트.
 *
 * <p>{@link QuestDefinition} 은 불변이라 화면에서 고치기 어렵다.
 * 그래서 편집하는 동안만 쓰는 고칠 수 있는 형태를 따로 둔다.
 * 저장할 때 다시 정의로 바꾼다.
 */
public final class QuestDraft {

    private final String id;
    private String display;
    private QuestType type = QuestType.NORMAL;
    private int requireLevel = 1;
    private String requireJob;
    private boolean repeatable;

    private final List<Objective> objectives = new ArrayList<>();

    private double combatExp;
    private int skillPoints;
    private int statPoints;
    private final Map<String, Long> currency = new LinkedHashMap<>();

    public QuestDraft(String id) {
        this.id = id;
        this.display = id;
    }

    /** 기존 퀘스트를 편집용으로 옮긴다. */
    public static QuestDraft from(QuestDefinition quest) {
        QuestDraft draft = new QuestDraft(quest.id());
        draft.display = quest.display();
        draft.type = quest.type();
        draft.requireLevel = quest.requireLevel();
        draft.requireJob = quest.requireJob();
        draft.repeatable = quest.repeatable();
        draft.objectives.addAll(quest.objectives());
        draft.combatExp = quest.reward().combatExp();
        draft.skillPoints = quest.reward().skillPoints();
        draft.statPoints = quest.reward().statPoints();
        draft.currency.putAll(quest.reward().currency());
        return draft;
    }

    /** 저장할 수 있는 상태인지. 목표가 하나도 없으면 저장하지 않는다. */
    public boolean isValid() {
        return !objectives.isEmpty();
    }

    public QuestDefinition toDefinition() {
        return new QuestDefinition(id, display, type, Math.max(1, requireLevel), requireJob,
                List.copyOf(objectives),
                new QuestReward(Math.max(0, combatExp), Math.max(0, skillPoints),
                        Math.max(0, statPoints), Map.copyOf(currency)),
                repeatable);
    }

    // ------------------------------------------------------------

    public String id() {
        return id;
    }

    public String display() {
        return display;
    }

    public void display(String display) {
        this.display = display == null || display.isBlank() ? id : display;
    }

    public QuestType type() {
        return type;
    }

    /** 다음 종류로 넘긴다. */
    public void cycleType() {
        QuestType[] values = QuestType.values();
        type = values[(type.ordinal() + 1) % values.length];
    }

    public int requireLevel() {
        return requireLevel;
    }

    public void requireLevel(int level) {
        this.requireLevel = Math.max(1, level);
    }

    public String requireJob() {
        return requireJob;
    }

    public void requireJob(String jobId) {
        this.requireJob = jobId;
    }

    public boolean repeatable() {
        return repeatable;
    }

    public void toggleRepeatable() {
        repeatable = !repeatable;
    }

    public List<Objective> objectives() {
        return objectives;
    }

    public void addObjective() {
        objectives.add(new Objective(ObjectiveType.KILL, "ZOMBIE", 1));
    }

    public void removeObjective(int index) {
        if (index >= 0 && index < objectives.size()) {
            objectives.remove(index);
        }
    }

    /** 목표의 종류를 다음 것으로 넘긴다. 대상은 그대로 둔다. */
    public void cycleObjectiveType(int index) {
        if (index < 0 || index >= objectives.size()) {
            return;
        }
        Objective current = objectives.get(index);
        ObjectiveType[] values = ObjectiveType.values();
        ObjectiveType next = values[(current.type().ordinal() + 1) % values.length];
        objectives.set(index, new Objective(next, current.key(), current.amount()));
    }

    /** 목표의 대상과 개수를 바꾼다. */
    public void setObjectiveTarget(int index, String key, int amount) {
        if (index < 0 || index >= objectives.size()) {
            return;
        }
        Objective current = objectives.get(index);
        objectives.set(index, new Objective(current.type(), key, amount));
    }

    public double combatExp() {
        return combatExp;
    }

    public void addCombatExp(double delta) {
        combatExp = Math.max(0, combatExp + delta);
    }

    public int skillPoints() {
        return skillPoints;
    }

    public void addSkillPoints(int delta) {
        skillPoints = Math.max(0, skillPoints + delta);
    }

    public int statPoints() {
        return statPoints;
    }

    public void addStatPoints(int delta) {
        statPoints = Math.max(0, statPoints + delta);
    }

    public Map<String, Long> currency() {
        return currency;
    }
}
