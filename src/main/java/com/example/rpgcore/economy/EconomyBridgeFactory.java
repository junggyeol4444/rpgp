package com.example.rpgcore.economy;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * 부팅할 때 어떤 경제 연동을 쓸지 고른다.
 *
 * <p>지시서 0장 6번: 경제 플러그인이 없어도 서버가 뜨고 그 기능만
 * 꺼져야 한다. 그래서 Vault 클래스는 플러그인이 실제로 있을 때만
 * 건드리고, 없을 때 나는 {@link LinkageError} 까지 잡는다.
 *
 * <p>VaultUnlocked 는 플러그인 이름을 {@code Vault} 로 등록하는 드롭인
 * 대체품이라 이름 검사 하나로 양쪽을 덮는다.
 */
public final class EconomyBridgeFactory {

    /** 원본 Vault 와 VaultUnlocked 가 공통으로 쓰는 플러그인 이름. */
    private static final String VAULT_PLUGIN = "Vault";

    private EconomyBridgeFactory() {
    }

    /**
     * 쓸 수 있는 연동을 찾는다. 없으면 {@link NoEconomyBridge}.
     *
     * @param preferUnlocked VaultUnlocked 의 새 API 를 먼저 찾을지
     */
    public static EconomyBridge resolve(Server server, boolean enabled,
                                        boolean preferUnlocked, Logger logger) {
        if (!enabled) {
            logger.info("경제 연동이 설정에서 꺼져 있습니다.");
            return new NoEconomyBridge();
        }
        if (server.getPluginManager().getPlugin(VAULT_PLUGIN) == null) {
            logger.info("Vault 계열 플러그인이 없어 일반 화폐 기능을 끕니다.");
            return new NoEconomyBridge();
        }

        EconomyBridge found = preferUnlocked
                ? firstOf(server, logger, EconomyBridgeFactory::tryUnlocked,
                        EconomyBridgeFactory::tryLegacy)
                : firstOf(server, logger, EconomyBridgeFactory::tryLegacy,
                        EconomyBridgeFactory::tryUnlocked);

        if (found == null) {
            logger.warning("Vault 는 있지만 등록된 경제 서비스가 없어 일반 화폐 기능을 끕니다.");
            return new NoEconomyBridge();
        }
        logger.info("경제 연동: " + found.name());
        return found;
    }

    private static EconomyBridge firstOf(Server server, Logger logger, Attempt... attempts) {
        for (Attempt attempt : attempts) {
            EconomyBridge bridge = attempt.run(server, logger);
            if (bridge != null && bridge.available()) {
                return bridge;
            }
        }
        return null;
    }

    /** VaultUnlocked 의 새 API. 없으면 null. */
    private static EconomyBridge tryUnlocked(Server server, Logger logger) {
        try {
            RegisteredServiceProvider<net.milkbowl.vault2.economy.Economy> registration =
                    server.getServicesManager()
                            .getRegistration(net.milkbowl.vault2.economy.Economy.class);
            return registration == null ? null : new UnlockedVaultEconomy(registration.getProvider());
        } catch (LinkageError | RuntimeException e) {
            // 원본 Vault 만 깔린 서버에서는 vault2 클래스 자체가 없다.
            logger.log(Level.FINE, "VaultUnlocked API 를 쓸 수 없습니다.", e);
            return null;
        }
    }

    /**
     * 원본 Vault 와 호환되는 레거시 API. 없으면 null.
     *
     * <p>이 인터페이스에는 사용 중단 표시가 붙어 있다. 그래도 쓰는 이유는
     * 원본 Vault 가 등록하는 것이 이 타입뿐이기 때문이다.
     * 기획서 8장이 양쪽을 모두 붙이라고 했으므로 의도한 사용이다.
     */
    @SuppressWarnings("deprecation")
    private static EconomyBridge tryLegacy(Server server, Logger logger) {
        try {
            RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> registration =
                    server.getServicesManager()
                            .getRegistration(net.milkbowl.vault.economy.Economy.class);
            return registration == null ? null : new LegacyVaultEconomy(registration.getProvider());
        } catch (LinkageError | RuntimeException e) {
            logger.log(Level.FINE, "Vault 레거시 API 를 쓸 수 없습니다.", e);
            return null;
        }
    }

    @FunctionalInterface
    private interface Attempt {
        EconomyBridge run(Server server, Logger logger);
    }
}
