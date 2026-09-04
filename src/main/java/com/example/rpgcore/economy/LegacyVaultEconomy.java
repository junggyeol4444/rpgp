package com.example.rpgcore.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

/**
 * 원본 Vault 의 {@code net.milkbowl.vault.economy.Economy} 어댑터.
 *
 * <p>VaultUnlocked 도 같은 인터페이스를 그대로 제공하므로 이 어댑터는
 * 양쪽 플러그인에서 모두 동작한다. (두 저장소의 Economy 인터페이스
 * 메서드 목록이 완전히 같은 것을 확인했다)
 *
 * <p>이 인터페이스에는 사용 중단 표시가 붙어 있다. 그래도 쓰는 이유는
 * 원본 Vault 가 등록하는 것이 이 타입뿐이기 때문이다. 새 API 를 쓰는
 * 경로는 {@link UnlockedVaultEconomy} 에 따로 있다.
 */
@SuppressWarnings("deprecation")
public final class LegacyVaultEconomy implements EconomyBridge {

    private final Economy economy;

    public LegacyVaultEconomy(Economy economy) {
        this.economy = economy;
    }

    @Override
    public String name() {
        return "Vault(" + economy.getName() + ")";
    }

    @Override
    public boolean available() {
        return economy.isEnabled();
    }

    @Override
    public double balance(OfflinePlayer player) {
        return economy.getBalance(player);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return amount <= 0 || economy.has(player, amount);
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (amount <= 0) {
            return true;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response != null && response.transactionSuccess();
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        if (amount <= 0) {
            return true;
        }
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response != null && response.transactionSuccess();
    }

    @Override
    public String format(double amount) {
        return economy.format(amount);
    }
}
