package com.example.rpgcore.quest;

import com.example.rpgcore.quest.objective.Objective;
import com.example.rpgcore.quest.reward.QuestReward;
import java.util.List;

/**
 * 지시서 3장 [quest/QuestDefinition] — 설정에서 읽은 퀘스트 정의.
 *
 * @param id           퀘스트 id
 * @param display      표시 이름
 * @param type         종류
 * @param requireLevel 수주에 필요한 전투 레벨
 * @param requireJob   필요한 기본 직업 id. 없으면 null
 * @param objectives   목표 목록. 순서가 진행도 저장 순서다
 * @param reward       보상
 * @param repeatable   완료한 뒤 다시 받을 수 있는지
 */
public record QuestDefinition(String id,
                              String display,
                              QuestType type,
                              int requireLevel,
                              String requireJob,
                              List<Objective> objectives,
                              QuestReward reward,
                              boolean repeatable) {

    public QuestDefinition {
        objectives = List.copyOf(objectives);
    }

    /** 주기마다 다시 받을 수 있는 종류이거나 repeatable 로 열어둔 퀘스트. */
    public boolean canRepeat() {
        return repeatable || type.cyclic();
    }
}
