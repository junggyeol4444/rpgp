package com.example.rpgcore.config.schema;

import com.example.rpgcore.life.TrackDefinition;
import com.example.rpgcore.life.TrackType;
import com.example.rpgcore.life.unlock.TrackReward;
import java.util.EnumMap;
import java.util.Map;

/**
 * 지시서 8장 [life.yml] 의 파싱 결과.
 *
 * @param tracks  트랙 정의
 * @param rewards 트랙별 레벨업 보상
 */
public record LifeSettings(Map<TrackType, TrackDefinition> tracks,
                           Map<TrackType, TrackReward> rewards) {

    public LifeSettings {
        tracks = new EnumMap<>(tracks);
        rewards = new EnumMap<>(rewards);
    }

    public static LifeSettings defaults() {
        return new LifeSettings(new EnumMap<>(TrackType.class), new EnumMap<>(TrackType.class));
    }

    /** 없으면 null. */
    public TrackDefinition track(TrackType type) {
        return tracks.get(type);
    }

    /** 없으면 빈 보상. */
    public TrackReward reward(TrackType type) {
        TrackReward reward = rewards.get(type);
        return reward == null ? TrackReward.none() : reward;
    }
}
