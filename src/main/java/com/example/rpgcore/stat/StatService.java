package com.example.rpgcore.stat;

import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.config.schema.ResetSettings;
import com.example.rpgcore.config.schema.StatSettings;
import com.example.rpgcore.config.validation.ValidationReport;
import com.example.rpgcore.core.Lifecycle;
import com.example.rpgcore.core.Reloadable;
import com.example.rpgcore.economy.CurrencyService;
import com.example.rpgcore.job.JobBranch;
import com.example.rpgcore.job.JobDefinition;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.player.data.PlayerData;
import com.example.rpgcore.storage.dirty.SavePriority;
import com.example.rpgcore.storage.dirty.SaveScheduler;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 지시서 3장 [stat/StatService] — 스탯 포인트 분배·회수와 파생 수치 환산.
 *
 * <p>기획서 4장: 능력치를 실제 수치로 바꾸는 2단 구조이며, 레벨업으로
 * 받은 포인트를 플레이어가 자유 분배한다. 초기화는 유료다.
 *
 * <p>분배 확정과 초기화는 되돌릴 수 없고 재화가 걸리므로 즉시 저장한다.
 * (지시서 5장 [저장 정책])
 */
public final class StatService implements Lifecycle, Reloadable {

    /** 분배·초기화 시도의 결과. */
    public enum Result {
        OK,
        /** stats.yml 에 없는 능력치 */
        UNKNOWN_STAT,
        /** 0 이하이거나 너무 큰 값 */
        INVALID_AMOUNT,
        /** 남은 스탯 포인트 부족 */
        NOT_ENOUGH_POINTS,
        /** 초기화가 막혀 있음 */
        RESET_NOT_ALLOWED,
        /** 초기화 비용 부족 */
        NOT_ENOUGH_CURRENCY,
        /** 되돌릴 분배 내역이 없음 */
        NOTHING_TO_RESET
    }

    private final ConfigManager config;
    private final SaveScheduler saves;
    private final PlayerManager players;
    private final CurrencyService currencies;

    /** 파생 수치가 다시 계산됐을 때 알림을 받을 대상. HP·UI 가 등록한다. */
    private Consumer<RpgPlayer> onRecalculated = rpgPlayer -> { };

    public StatService(ConfigManager config, SaveScheduler saves, PlayerManager players,
                       CurrencyService currencies) {
        this.config = config;
        this.saves = saves;
        this.players = players;
        this.currencies = currencies;
    }

    @Override
    public String serviceName() {
        return "StatService";
    }

    /** 파생 수치가 바뀔 때 호출될 대상을 더한다. */
    public void onRecalculated(Consumer<RpgPlayer> listener) {
        Consumer<RpgPlayer> previous = this.onRecalculated;
        this.onRecalculated = rpgPlayer -> {
            previous.accept(rpgPlayer);
            listener.accept(rpgPlayer);
        };
    }

    @Override
    public void reload(ValidationReport report) {
        // 환산 계수가 바뀌었을 수 있으므로 접속 중인 전원을 다시 계산한다.
        for (RpgPlayer rpgPlayer : players.onlinePlayers()) {
            refresh(rpgPlayer);
        }
    }

    public StatSettings settings() {
        return config.stats();
    }

    // ------------------------------------------------------------
    // 환산
    // ------------------------------------------------------------

    /**
     * 능력치를 파생 수치로 환산한다.
     *
     * <p>기본값 + (분배한 포인트 x 포인트당 증가량).
     */
    public DerivedStats compute(PlayerData data) {
        StatSettings settings = config.stats();
        DerivedStats.Builder builder = DerivedStats.builder();

        for (DerivedStat stat : DerivedStat.values()) {
            builder.set(stat, settings.base(stat));
        }
        for (Map.Entry<String, StatType> entry : settings.stats().entrySet()) {
            int points = data.combat().stat(entry.getKey()) + jobBonus(data, entry.getKey());
            if (points <= 0) {
                continue;
            }
            for (Map.Entry<DerivedStat, Double> derived : entry.getValue().perPoint().entrySet()) {
                builder.add(derived.getKey(), derived.getValue() * points);
            }
        }

        // TODO 스킬·장비로 붙는 보정을 여기에 더한다.

        return builder.build();
    }

    /**
     * 직업이 주는 능력치 보정.
     *
     * <p>기획서 5장 [직업간 차별화 축]: 레벨업 시 스탯 보정 차이.
     * jobs.yml 의 statBonusPerLevel 을 전투 레벨만큼 곱한다.
     * 1차·2차 전직을 했으면 그 분기 보정이 기본 직업 보정에 더해진다.
     *
     * <p>[확인 필요 — 밸런스]
     * 기준을 "전투 레벨 전체"로 잡았다. 직업은 3레벨에 고르므로
     * 고르기 전 레벨분도 함께 들어간다. 기획서에 기준이 적혀 있지
     * 않아 가장 단순한 쪽을 택했다. 선택 이후 레벨만 세는 쪽이
     * 맞다면 이 메서드만 고치면 된다.
     */
    /**
     * 능력치 실제 값. 분배한 포인트 + 직업 보정.
     * 스킬의 statScaling 이 이 값을 쓴다.
     */
    public int abilityPoints(PlayerData data, String statId) {
        return data.combat().stat(statId) + jobBonus(data, statId);
    }

    private int jobBonus(PlayerData data, String statId) {
        JobDefinition base = config.jobs().tree().base(data.job().base());
        if (base == null) {
            return 0;
        }
        int perLevel = base.statBonusPerLevel(statId);

        // 전직을 했으면 그 분기 보정이 기본 직업 보정에 더해진다. (8·9단계)
        JobBranch tier1 = config.jobs().tree().tier1(data.job().base(), data.job().tier1());
        if (tier1 != null) {
            perLevel += tier1.statBonusPerLevel(statId);
        }
        JobBranch tier2 = config.jobs().tree()
                .tier2(data.job().base(), data.job().tier1(), data.job().tier2());
        if (tier2 != null) {
            perLevel += tier2.statBonusPerLevel(statId);
        }
        return perLevel <= 0 ? 0 : perLevel * data.combat().level();
    }

    /** 캐시를 다시 만들고 등록된 쪽에 알린다. */
    public void refresh(RpgPlayer rpgPlayer) {
        rpgPlayer.derived(compute(rpgPlayer.data()));
        onRecalculated.accept(rpgPlayer);
    }

    // ------------------------------------------------------------
    // 분배
    // ------------------------------------------------------------

    /**
     * 스탯 포인트를 분배한다.
     *
     * @param amount 1 이상
     */
    public Result allocate(RpgPlayer rpgPlayer, String statId, int amount) {
        if (amount <= 0) {
            return Result.INVALID_AMOUNT;
        }
        if (config.stats().stat(statId) == null) {
            return Result.UNKNOWN_STAT;
        }
        PlayerData.Combat combat = rpgPlayer.data().combat();
        if (combat.statPoints() < amount) {
            return Result.NOT_ENOUGH_POINTS;
        }
        combat.statPoints(combat.statPoints() - amount);
        combat.stats().merge(statId, amount, Integer::sum);

        saves.markDirty(rpgPlayer.data(), SavePriority.IMMEDIATE);
        refresh(rpgPlayer);
        return Result.OK;
    }

    /** 이번 초기화에 드는 비용. */
    public long resetCost(PlayerData data) {
        return config.stats().reset().costFor(data.combat().statResetCount());
    }

    /**
     * 분배 내역을 전부 되돌린다. 비용을 특수 재화로 받는다.
     *
     * <p>재화 차감은 {@link CurrencyService} 를 거친다. (6단계)
     */
    public Result reset(RpgPlayer rpgPlayer) {
        ResetSettings reset = config.stats().reset();
        if (!reset.allowed()) {
            return Result.RESET_NOT_ALLOWED;
        }
        PlayerData data = rpgPlayer.data();
        PlayerData.Combat combat = data.combat();

        int refund = 0;
        for (int points : combat.stats().values()) {
            refund += Math.max(0, points);
        }
        if (refund <= 0) {
            return Result.NOTHING_TO_RESET;
        }

        long cost = reset.costFor(combat.statResetCount());
        if (cost > 0
                && currencies.withdraw(rpgPlayer, reset.currencyId(), cost)
                        != CurrencyService.Result.OK) {
            return Result.NOT_ENOUGH_CURRENCY;
        }

        combat.stats().clear();
        combat.statPoints(combat.statPoints() + refund);
        combat.statResetCount(combat.statResetCount() + 1);

        saves.markDirty(data, SavePriority.IMMEDIATE);
        refresh(rpgPlayer);
        return Result.OK;
    }

    /**
     * 관리자용 강제 초기화. 비용을 받지 않는다. (/rpg admin statreset)
     * 초기화 횟수도 올리지 않는다.
     */
    public void forceReset(RpgPlayer rpgPlayer) {
        PlayerData.Combat combat = rpgPlayer.data().combat();
        int refund = 0;
        for (int points : combat.stats().values()) {
            refund += Math.max(0, points);
        }
        combat.stats().clear();
        combat.statPoints(combat.statPoints() + refund);

        saves.markDirty(rpgPlayer.data(), SavePriority.IMMEDIATE);
        refresh(rpgPlayer);
    }

    /** 관리자용 포인트 지급·회수. 음수면 회수한다. */
    public void addPoints(RpgPlayer rpgPlayer, int amount) {
        PlayerData.Combat combat = rpgPlayer.data().combat();
        combat.statPoints(Math.max(0, combat.statPoints() + amount));
        saves.markDirty(rpgPlayer.data(), SavePriority.IMMEDIATE);
    }
}
