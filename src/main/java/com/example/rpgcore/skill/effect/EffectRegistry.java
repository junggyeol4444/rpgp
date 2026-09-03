package com.example.rpgcore.skill.effect;

import java.util.EnumMap;
import java.util.Map;

/** 효과 타입 -> 실행기. 부팅할 때 채운다. */
public final class EffectRegistry {

    private final Map<EffectType, EffectExecutor> executors = new EnumMap<>(EffectType.class);

    public EffectRegistry register(EffectExecutor executor) {
        executors.put(executor.type(), executor);
        return this;
    }

    /** 없으면 null. */
    public EffectExecutor get(EffectType type) {
        return executors.get(type);
    }

    public boolean covers(EffectType type) {
        return executors.containsKey(type);
    }

    public int size() {
        return executors.size();
    }
}
