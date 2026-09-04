package com.example.rpgcore.economy;

import org.bukkit.OfflinePlayer;

/**
 * 지시서 3장 [economy/EconomyBridge] — 일반 화폐 어댑터.
 *
 * <p>기획서 8장: 일반 화폐는 Vault 로 기존 경제 플러그인에 연동하고,
 * Vault 와 VaultUnlocked 양쪽을 모두 붙인다.
 *
 * <p>지시서 0장 6번: 경제 플러그인이 없어도 서버가 뜨고 이 기능만
 * 꺼져야 한다. 그래서 {@link #available()} 이 false 인 구현을 기본으로 둔다.
 *
 * <p>이 인터페이스 밖으로 Vault 타입이 새어나가지 않는다.
 * 나머지 코드는 어느 쪽 API 가 붙었는지 알 필요가 없다.
 */
public interface EconomyBridge {

    /** 어떤 구현인지. /rpg admin status 에 표시한다. */
    String name();

    /** 지금 쓸 수 있는지. */
    boolean available();

    /** 잔액. 쓸 수 없으면 0. */
    double balance(OfflinePlayer player);

    /** 잔액이 충분한지. */
    boolean has(OfflinePlayer player, double amount);

    /**
     * 돈을 뺀다.
     *
     * @return 실제로 빠졌으면 true
     */
    boolean withdraw(OfflinePlayer player, double amount);

    /**
     * 돈을 넣는다.
     *
     * @return 실제로 들어갔으면 true
     */
    boolean deposit(OfflinePlayer player, double amount);

    /** 표시용 문자열. */
    String format(double amount);
}
