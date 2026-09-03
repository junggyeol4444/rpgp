package com.example.rpgcore.npc;

import org.bukkit.entity.Entity;

/**
 * 지시서 3장 [npc/NpcBridge] — NPC 추상 인터페이스.
 *
 * <p>지시서 0장 6번: 외부 플러그인 의존은 전부 soft-depend 다.
 * NPC 플러그인이 없어도 서버가 뜨고 그 기능만 꺼져야 한다.
 *
 * <p>[확인 필요 - 지시서 16장 6번]
 * Citizens 의 26.x 실제 구동 여부가 확인되지 않았다. 확인 전까지
 * Citizens 구현을 만들지 않고 인터페이스만 둔다. (지시서 0장 5번)
 * 확인되면 이 인터페이스를 구현하는 클래스를 npc/citizens 에 넣고
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
