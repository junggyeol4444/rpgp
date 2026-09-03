package com.example.rpgcore.storage;

import com.example.rpgcore.player.data.PlayerData;
import java.util.UUID;

/**
 * 지시서 5장 [인터페이스].
 *
 * <p>저장소 추상화 계층. 이후 SQLite 등으로 갈아끼울 수 있도록,
 * 구현에 쓰인 타입(YAML 등)이 이 인터페이스 밖으로 새어나가면 안 된다.
 *
 * <p>모든 메서드는 디스크에 닿으므로 메인 스레드에서 호출하지 않는다.
 * (지시서 0장 4번) 호출 지점은 저장 스케줄러 한 곳으로 모은다.
 */
public interface PlayerDataRepository {

    /** 저장소 이름. /rpg admin status 에 표시한다. */
    String storageType();

    /**
     * 데이터를 읽는다. 저장된 것이 없으면 기본값이 채워진 새 데이터를 준다.
     * 호출 스레드를 블로킹한다.
     */
    PlayerData load(UUID uuid);

    /** 한 명을 저장한다. 호출 스레드를 블로킹한다. */
    void save(PlayerData data);

    /**
     * 구현체가 들고 있는 대기 중인 쓰기를 전부 마무리한다.
     *
     * <p>어떤 플레이어를 저장할지 고르는 것은 캐시·스케줄러의 몫이고,
     * 이 메서드는 이미 넘겨받은 쓰기를 끝까지 밀어넣는 역할만 한다.
     * 서버 종료 시 마지막에 한 번 호출한다.
     */
    void saveAll();

    /** 저장된 데이터가 있는지. */
    boolean exists(UUID uuid);

    /** 저장된 데이터를 지운다. (/rpg admin datareset) */
    void delete(UUID uuid);
}
