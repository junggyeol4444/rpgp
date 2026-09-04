package com.example.rpgcore.npc;

import org.bukkit.entity.Entity;

/**
 * NPC 플러그인이 없을 때 쓰는 구현.
 *
 * <p>아무 개체도 NPC 로 보지 않는다. 대화(TALK) 목표는 진행되지 않고,
 * 나머지 기능은 그대로 돈다.
 */
public final class NoNpcBridge implements NpcBridge {

    @Override
    public String name() {
        return "none";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public String npcIdOf(Entity entity) {
        return null;
    }
}
