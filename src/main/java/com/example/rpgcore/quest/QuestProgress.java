package com.example.rpgcore.quest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 진행 중인 퀘스트 하나의 상태.
 *
 * <p>지시서 7장 스키마의 {@code quest.active} 안에 이 모양으로 들어간다.
 * 구조는 5단계에서 확정한다고 되어 있었고, 아래가 그 결과다.
 *
 * <pre>
 * quest:
 *   active:
 *     example_quest:
 *       startedAt: 0
 *       counts: [3, 0]
 * </pre>
 *
 * <p>{@code counts} 는 정의의 목표 순서를 그대로 따른다.
 */
public final class QuestProgress {

    private final long startedAt;
    private final int[] counts;

    public QuestProgress(long startedAt, int objectiveCount) {
        this.startedAt = startedAt;
        this.counts = new int[Math.max(0, objectiveCount)];
    }

    private QuestProgress(long startedAt, int[] counts) {
        this.startedAt = startedAt;
        this.counts = counts;
    }

    public long startedAt() {
        return startedAt;
    }

    public int size() {
        return counts.length;
    }

    public int count(int index) {
        return index >= 0 && index < counts.length ? counts[index] : 0;
    }

    /** 진행도를 올린다. 값이 바뀌었으면 true. */
    public boolean advance(int index, int amount) {
        if (index < 0 || index >= counts.length || amount <= 0) {
            return false;
        }
        counts[index] += amount;
        return true;
    }

    public void set(int index, int value) {
        if (index >= 0 && index < counts.length) {
            counts[index] = Math.max(0, value);
        }
    }

    /**
     * 저장 파일에서 읽는다. 목표 개수가 바뀌었으면 새 길이에 맞춰
     * 앞에서부터 옮긴다. (정의를 고쳐도 진행 중인 퀘스트가 깨지지 않게)
     */
    public static QuestProgress fromMap(Map<String, Object> raw, int objectiveCount) {
        long startedAt = 0;
        int[] counts = new int[Math.max(0, objectiveCount)];
        if (raw != null) {
            Object started = raw.get("startedAt");
            if (started instanceof Number number) {
                startedAt = number.longValue();
            }
            if (raw.get("counts") instanceof List<?> list) {
                for (int i = 0; i < counts.length && i < list.size(); i++) {
                    if (list.get(i) instanceof Number number) {
                        counts[i] = Math.max(0, number.intValue());
                    }
                }
            }
        }
        return new QuestProgress(startedAt, counts);
    }

    /** 저장용 Map. */
    public Map<String, Object> toMap() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("startedAt", startedAt);
        List<Integer> list = new ArrayList<>(counts.length);
        for (int count : counts) {
            list.add(count);
        }
        raw.put("counts", list);
        return raw;
    }
}
