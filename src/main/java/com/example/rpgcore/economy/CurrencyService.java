package com.example.rpgcore.economy;

import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.config.schema.CurrencyDefinition;
import com.example.rpgcore.core.Lifecycle;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.player.data.PlayerData;
import com.example.rpgcore.storage.dirty.SavePriority;
import com.example.rpgcore.storage.dirty.SaveScheduler;
import java.util.Collection;

/**
 * 지시서 3장 [economy/CurrencyService] — 특수 재화 자체 관리.
 *
 * <p>기획서 8장: 특수 재화(던전 코인 등)는 외부 경제 플러그인에 맡기지
 * 않고 플러그인 안에서 직접 들고 있는다. 그래서 경제 플러그인이 없어도
 * 이 기능은 그대로 돈다.
 *
 * <p>재화 증감은 즉시 저장 대상이다. (지시서 5장 [저장 정책])
 */
public final class CurrencyService implements Lifecycle {

    /** 증감 시도의 결과. */
    public enum Result {
        OK,
        /** economy.yml 에 없는 재화 */
        UNKNOWN_CURRENCY,
        /** 0 이하 */
        INVALID_AMOUNT,
        /** 보유량 부족 */
        NOT_ENOUGH
    }

    private final ConfigManager config;
    private final SaveScheduler saves;

    public CurrencyService(ConfigManager config, SaveScheduler saves) {
        this.config = config;
        this.saves = saves;
    }

    @Override
    public String serviceName() {
        return "CurrencyService";
    }

    public Collection<CurrencyDefinition> all() {
        return config.economy().currencies().values();
    }

    /** 없으면 null. */
    public CurrencyDefinition definition(String currencyId) {
        return config.economy().currency(currencyId);
    }

    /** 표시 이름. 정의가 없으면 id 를 그대로 준다. */
    public String display(String currencyId) {
        CurrencyDefinition definition = definition(currencyId);
        return definition == null ? String.valueOf(currencyId) : definition.display();
    }

    public long balance(PlayerData data, String currencyId) {
        return data.currency(currencyId);
    }

    public boolean has(PlayerData data, String currencyId, long amount) {
        return amount <= 0 || balance(data, currencyId) >= amount;
    }

    /** 재화를 뺀다. */
    public Result withdraw(RpgPlayer rpgPlayer, String currencyId, long amount) {
        if (definition(currencyId) == null) {
            return Result.UNKNOWN_CURRENCY;
        }
        if (amount <= 0) {
            return Result.INVALID_AMOUNT;
        }
        PlayerData data = rpgPlayer.data();
        long owned = balance(data, currencyId);
        if (owned < amount) {
            return Result.NOT_ENOUGH;
        }
        data.currency().put(currencyId, owned - amount);
        saves.markDirty(data, SavePriority.IMMEDIATE);
        return Result.OK;
    }

    /** 재화를 넣는다. 상한이 있으면 잘린다. */
    public Result deposit(RpgPlayer rpgPlayer, String currencyId, long amount) {
        CurrencyDefinition definition = definition(currencyId);
        if (definition == null) {
            return Result.UNKNOWN_CURRENCY;
        }
        if (amount <= 0) {
            return Result.INVALID_AMOUNT;
        }
        PlayerData data = rpgPlayer.data();
        long owned = balance(data, currencyId);
        // 더하다가 long 을 넘기지 않도록 미리 막는다.
        long next = owned > Long.MAX_VALUE - amount ? Long.MAX_VALUE : owned + amount;
        data.currency().put(currencyId, definition.clamp(next));
        saves.markDirty(data, SavePriority.IMMEDIATE);
        return Result.OK;
    }

    /**
     * 관리자용 증감. 정의가 없어도 처리하고, 모자라면 0까지만 뺀다.
     * (/rpg admin currency)
     */
    public void adjust(RpgPlayer rpgPlayer, String currencyId, long delta) {
        PlayerData data = rpgPlayer.data();
        long owned = balance(data, currencyId);
        long next;
        if (delta >= 0) {
            next = owned > Long.MAX_VALUE - delta ? Long.MAX_VALUE : owned + delta;
        } else {
            next = Math.max(0, owned + delta);
        }
        CurrencyDefinition definition = definition(currencyId);
        data.currency().put(currencyId, definition == null ? next : definition.clamp(next));
        saves.markDirty(data, SavePriority.IMMEDIATE);
    }
}
