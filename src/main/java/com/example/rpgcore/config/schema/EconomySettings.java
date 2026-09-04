package com.example.rpgcore.config.schema;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 지시서 8장 [economy.yml] 의 파싱 결과.
 *
 * @param vaultEnabled   Vault 연동을 쓸지
 * @param preferUnlocked VaultUnlocked 의 새 API 를 먼저 찾을지
 * @param currencies     특수 재화 id -> 정의
 */
public record EconomySettings(boolean vaultEnabled,
                              boolean preferUnlocked,
                              Map<String, CurrencyDefinition> currencies) {

    public EconomySettings {
        currencies = new LinkedHashMap<>(currencies);
    }

    public static EconomySettings defaults() {
        return new EconomySettings(true, true, Map.of());
    }

    /** 없으면 null. */
    public CurrencyDefinition currency(String id) {
        return id == null ? null : currencies.get(id);
    }
}
