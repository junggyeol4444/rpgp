package com.example.rpgcore.economy;

import com.example.rpgcore.util.PluginIds;
import java.math.BigDecimal;
import java.math.RoundingMode;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

/**
 * VaultUnlocked 의 {@code net.milkbowl.vault2.economy.Economy} 어댑터.
 *
 * <p>새 API 는 UUID 와 BigDecimal 을 쓰고, 호출하는 플러그인 이름을
 * 함께 넘긴다. 멀티 화폐를 지원하지만 여기서는 기본 화폐만 쓴다.
 * (기획서 8장의 "일반 화폐" 하나에 해당한다)
 */
public final class UnlockedVaultEconomy implements EconomyBridge {

    /** 소수 자리. 표시용으로만 쓴다. */
    private static final int SCALE = 4;

    private final Economy economy;

    public UnlockedVaultEconomy(Economy economy) {
        this.economy = economy;
    }

    @Override
    public String name() {
        return "VaultUnlocked(" + economy.getName() + ")";
    }

    @Override
    public boolean available() {
        return economy.isEnabled();
    }

    @Override
    public double balance(OfflinePlayer player) {
        BigDecimal value = economy.balance(PluginIds.PLUGIN_NAME, player.getUniqueId());
        return value == null ? 0 : value.doubleValue();
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return amount <= 0 || balance(player) >= amount;
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (amount <= 0) {
            return true;
        }
        EconomyResponse response = economy.withdraw(
                PluginIds.PLUGIN_NAME, player.getUniqueId(), toDecimal(amount));
        return response != null && response.transactionSuccess();
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        if (amount <= 0) {
            return true;
        }
        EconomyResponse response = economy.deposit(
                PluginIds.PLUGIN_NAME, player.getUniqueId(), toDecimal(amount));
        return response != null && response.transactionSuccess();
    }

    @Override
    public String format(double amount) {
        return economy.format(PluginIds.PLUGIN_NAME, toDecimal(amount));
    }

    private static BigDecimal toDecimal(double amount) {
        return BigDecimal.valueOf(amount).setScale(SCALE, RoundingMode.HALF_UP);
    }
}
