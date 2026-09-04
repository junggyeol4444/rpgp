package com.example.rpgcore.npc;

import org.bukkit.entity.Entity;

/**
 * 지시서 3장 [npc/NpcBridge] — NPC 추상 인터페이스.
 *
 * <p>지시서 0장 6번: 외부 플러그인 의존은 전부 soft-depend 다.
 * NPC 플러그인이 없어도 서버가 뜨고 그 기능만 꺼져야 한다.
 *
 * <p>[지시서 16장 6번 확인] Citizens2 저장소에 26.1 · 26.2 용 NMS 모듈
 * (v26_1_R1, v26_2_R1)이 있다. 즉 Citizens 는 26.x 를 지원한다.
 * 다만 실제로 붙여서 돌려 본 것은 아니라 아직 구현을 만들지 않았다.
 * 구현할 때는 이 인터페이스를 구현하는 클래스를 npc/citizens 에 넣고
 * 부팅할 때 갈아끼우면 된다.
 */
public interface NpcBridge {

    /** 구현 이름. /rpg admin status 에 표시한다. */
    String name();

    /** 지금 NPC 연동을 쓸 수 있는지. */
    boolean available();

    /**
     * 상호작용한 개체의 NPC id.
     *
     * @return NPC 가 아니거나 연동이 없으면 null
     */
    String npcIdOf(Entity entity);
}
