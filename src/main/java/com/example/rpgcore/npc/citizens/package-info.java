/**
 * Citizens 연동 구현. soft-depend 이므로 없어도 서버가 떠야 한다.
 *
 * <p>지시서 3장 [npc/citizens] 의 자리다.
 *
 * <p>아직 비어 있다. 지시서 16장 6번(Citizens 의 26.x 지원)은
 * {@code tools/verify-against-paper.sh} 로 확인했고, Citizens2 저장소에
 * {@code v26_1_R1} · {@code v26_2_R1} NMS 모듈이 있다. 다만 연동 코드는
 * 실제 서버에서 이벤트 흐름을 확인해야 쓸 수 있어서 넣지 않았다.
 * 그때까지는 {@link com.example.rpgcore.npc.NoNpcBridge} 를 쓴다.
 */
package com.example.rpgcore.npc.citizens;
