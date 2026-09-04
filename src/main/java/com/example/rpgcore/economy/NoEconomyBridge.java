package com.example.rpgcore.economy;

import org.bukkit.OfflinePlayer;

/**
 * 경제 플러그인이 없을 때 쓰는 구현.
 *
 * <p>일반 화폐 기능만 꺼지고 나머지는 그대로 돈다.
 * 특수 재화는 플러그인이 직접 들고 있으므로 영향을 받지 않는다.
 */
public final class NoEconomyBridge implements EconomyBridge {

    @Override
    public String name() {
        return "none";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public double balance(OfflinePlayer player) {
        return 0;
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return false;
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        return false;
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        return false;
    }

    @Override
    public String format(double amount) {
        return String.valueOf(amount);
    }
}
