package com.example.rpgcore.level;

import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.config.schema.CurveSettings;
import com.example.rpgcore.config.schema.LevelSettings;
import com.example.rpgcore.config.validation.ValidationReport;
import com.example.rpgcore.core.Lifecycle;
import com.example.rpgcore.core.Reloadable;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.player.data.PlayerData;
import com.example.rpgcore.storage.dirty.SavePriority;
import com.example.rpgcore.storage.dirty.SaveScheduler;
import com.example.rpgcore.util.Messages;
import java.util.function.Consumer;

/**
 * 지시서 3장 [level/CombatLevelService] — 전투 레벨·경험치.
 *
 * <p>기획서 3장: 바닐라 경험치와 완전히 분리한 별도 RPG 경험치를 쓴다.
 * 레벨 상한은 없고 요구 경험치는 지수형이다.
 *
 * <p>저장 등급은 지연 저장이다. (지시서 5장 [저장 정책])
 */
public final class CombatLevelService implements Lifecycle, Reloadable {

    /**
     * 한 번에 처리할 수 있는 레벨업 횟수 상한.
     * 설정을 잘못 넣어 요구치가 0에 수렴해도 서버가 멈추지 않도록 둔 안전장치다.
     */
    private static final int MAX_LEVELUPS_PER_CALL = 10_000;

    private final ConfigManager config;
    private final SaveScheduler saves;
    private final Messages messages;

    private volatile ExpCurve curve = new ExpCurve.Exponential(100.0, 1.12);

    /**
     * 레벨이 바뀌었을 때 불릴 대상.
     * 직업 보정이 레벨에 비례하므로(3단계) 파생 수치를 다시 계산해야 한다.
     */
    private Consumer<RpgPlayer> onLevelChanged = rpgPlayer -> { };

    public CombatLevelService(ConfigManager config, SaveScheduler saves, Messages messages) {
        this.config = config;
        this.saves = saves;
        this.messages = messages;
    }

    @Override
    public String serviceName() {
        return "CombatLevelService";
    }

    @Override
    public void enable() {
        rebuildCurve(null);
    }

    @Override
    public void reload(ValidationReport report) {
        rebuildCurve(report);
    }

    private void rebuildCurve(ValidationReport report) {
        curve = ExpCurve.from(config.levels().combatCurve(), CurveSettings.defaultCombat(),
                "levels.yml", "combat.curve", report);
    }

    /** 레벨이 바뀔 때 불릴 대상을 더한다. 등록 순서대로 불린다. */
    public void onLevelChanged(Consumer<RpgPlayer> listener) {
        Consumer<RpgPlayer> previous = this.onLevelChanged;
        this.onLevelChanged = rpgPlayer -> {
            previous.accept(rpgPlayer);
            listener.accept(rpgPlayer);
        };
    }

    /** 지금 쓰이는 곡선. */
    public ExpCurve curve() {
        return curve;
    }

    /** 지정한 레벨에서 다음 레벨로 가는 데 필요한 경험치. */
    public double requiredExp(int level) {
        return curve.requiredExp(level);
    }

    /** 더 오를 수 없는 상태인지. 상한이 없으면 항상 false. */
    public boolean isMaxLevel(PlayerData data) {
        LevelSettings settings = config.levels();
        return settings.hasCombatMaxLevel() && data.combat().level() >= settings.combatMaxLevel();
    }

    /**
     * 경험치를 준다.
     *
     * @param target 대상
     * @param amount 지급량. 0 이하면 아무 일도 하지 않는다
     * @param source 획득원. levels.yml 에서 꺼 둔 획득원이면 무시한다
     * @return 처리 결과
     */
    public Result addExp(RpgPlayer target, double amount, ExpSource source) {
        PlayerData data = target.data();
        LevelSettings settings = config.levels();

        if (amount <= 0 || !Double.isFinite(amount)) {
            return Result.none(data.combat().level());
        }
        if (!settings.isEnabled(source)) {
            return Result.none(data.combat().level());
        }
        if (isMaxLevel(data)) {
            messages.send(target.player(), "level.max-reached");
            return Result.none(data.combat().level());
        }

        PlayerData.Combat combat = data.combat();
        int before = combat.level();
        combat.exp(combat.exp() + amount);

        int statPointsGained = 0;
        int skillPointsGained = 0;
        int loops = 0;

        while (loops++ < MAX_LEVELUPS_PER_CALL) {
            if (settings.hasCombatMaxLevel() && combat.level() >= settings.combatMaxLevel()) {
                // 상한에 닿으면 남은 경험치는 버린다.
                combat.exp(0);
                break;
            }
            double required = curve.requiredExp(combat.level());
            if (!(required > 0) || !Double.isFinite(required)) {
                // 곡선이 망가진 경우. 레벨을 올리지 않고 멈춘다.
                break;
            }
            if (combat.exp() < required) {
                break;
            }
            combat.exp(combat.exp() - required);
            combat.level(combat.level() + 1);
            combat.statPoints(combat.statPoints() + settings.statPointsPerLevel());
            data.skill().points(data.skill().points() + settings.skillPointsPerLevel());
            statPointsGained += settings.statPointsPerLevel();
            skillPointsGained += settings.skillPointsPerLevel();
        }

        saves.markDirty(data, SavePriority.DEFERRED);

        int after = combat.level();
        messages.send(target.player(), "level.exp-gain", "amount", format(amount));
        if (after > before) {
            messages.send(target.player(), "level.up", "before", before, "after", after);
            onLevelChanged.accept(target);
        }
        return new Result(before, after, amount, statPointsGained, skillPointsGained);
    }

    /**
     * 레벨을 직접 지정한다. (/rpg admin setlevel)
     * 남은 경험치는 0으로 되돌린다.
     */
    public void setLevel(RpgPlayer target, int level) {
        PlayerData data = target.data();
        int clamped = Math.max(1, level);
        LevelSettings settings = config.levels();
        if (settings.hasCombatMaxLevel()) {
            clamped = Math.min(clamped, settings.combatMaxLevel());
        }
        boolean changed = data.combat().level() != clamped;
        data.combat().level(clamped);
        data.combat().exp(0);
        saves.markDirty(data, SavePriority.DEFERRED);
        if (changed) {
            onLevelChanged.accept(target);
        }
    }

    /** 경험치를 직접 더하거나 뺀다. (/rpg admin exp) 레벨업 판정은 하지 않는다. */
    public void addRawExp(RpgPlayer target, double amount) {
        PlayerData data = target.data();
        double next = data.combat().exp() + amount;
        data.combat().exp(Math.max(0, next));
        saves.markDirty(data, SavePriority.DEFERRED);
    }

    /** 표시용 숫자 다듬기. 소수점 아래는 버린다. */
    public static String format(double value) {
        if (value >= 1e15) {
            return String.format("%.3e", value);
        }
        return String.valueOf((long) value);
    }

    /**
     * 경험치 지급 결과.
     *
     * @param before            지급 전 레벨
     * @param after             지급 후 레벨
     * @param gainedExp         지급한 경험치
     * @param gainedStatPoints  이번에 받은 스탯 포인트
     * @param gainedSkillPoints 이번에 받은 스킬 포인트
     */
    public record Result(int before, int after, double gainedExp,
                         int gainedStatPoints, int gainedSkillPoints) {

        static Result none(int level) {
            return new Result(level, level, 0, 0, 0);
        }

        public boolean leveledUp() {
            return after > before;
        }
    }
}
