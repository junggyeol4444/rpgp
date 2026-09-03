package com.example.rpgcore.skill;

import com.example.rpgcore.combat.DamagePipeline;
import com.example.rpgcore.combat.HealthService;
import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.core.Lifecycle;
import com.example.rpgcore.player.PlayerManager;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.player.data.PlayerData;
import com.example.rpgcore.skill.cooldown.CooldownService;
import com.example.rpgcore.skill.effect.EffectExecutor;
import com.example.rpgcore.skill.effect.EffectRegistry;
import com.example.rpgcore.skill.effect.SkillContext;
import com.example.rpgcore.skill.effect.SkillEffect;
import com.example.rpgcore.skill.mana.ManaService;
import com.example.rpgcore.stat.StatService;
import com.example.rpgcore.storage.dirty.SavePriority;
import com.example.rpgcore.storage.dirty.SaveScheduler;
import java.util.Map;

/**
 * 지시서 3장 [skill/SkillService] — 해금 · 레벨 · 발동.
 *
 * <p>기획서 6장 [스킬트리]: 해금은 분기 트리를 따라가고, 해금한 스킬의
 * 레벨은 자유 포인트로 올린다. 분기 선택은 되돌릴 수 없다.
 *
 * <p>해금과 포인트 소비는 되돌릴 수 없으므로 즉시 저장한다.
 * (지시서 5장 [저장 정책] / 기획서 9장)
 */
public final class SkillService implements Lifecycle {

    /** 해금 · 투자 · 시전 시도의 결과. */
    public enum Result {
        OK,
        /** 그런 스킬이 없음 */
        UNKNOWN_SKILL,
        /** 다른 직업의 스킬 */
        WRONG_JOB,
        /** 전직 단계가 모자람 */
        STAGE_LOCKED,
        /** 선행 스킬을 아직 해금하지 않음 */
        PARENT_LOCKED,
        /** 같은 분기에서 다른 쪽을 이미 골라 영구 잠금 */
        BRANCH_LOCKED,
        /** 이미 해금함 */
        ALREADY_UNLOCKED,
        /** 아직 해금하지 않음 */
        NOT_UNLOCKED,
        /** 스킬 포인트가 모자람 */
        NOT_ENOUGH_POINTS,
        /** 스킬 레벨 상한 */
        MAX_LEVEL,
        /** 쿨타임 중 */
        ON_COOLDOWN,
        /** 마나 부족 */
        NOT_ENOUGH_MANA,
        /** 실행기가 없어 아무 일도 일어나지 않음 */
        NO_EFFECT,
        /** 값이 올바르지 않음 */
        INVALID_AMOUNT
    }

    private final ConfigManager config;
    private final SaveScheduler saves;
    private final StatService stats;
    private final ManaService mana;
    private final CooldownService cooldowns;
    private final DamagePipeline pipeline;
    private final HealthService health;
    private final PlayerManager players;
    private final EffectRegistry effects;

    public SkillService(ConfigManager config, SaveScheduler saves, StatService stats,
                        ManaService mana, CooldownService cooldowns, DamagePipeline pipeline,
                        HealthService health, PlayerManager players, EffectRegistry effects) {
        this.config = config;
        this.saves = saves;
        this.stats = stats;
        this.mana = mana;
        this.cooldowns = cooldowns;
        this.pipeline = pipeline;
        this.health = health;
        this.players = players;
        this.effects = effects;
    }

    @Override
    public String serviceName() {
        return "SkillService";
    }

    public SkillTree tree() {
        return config.skillTree();
    }

    public CooldownService cooldowns() {
        return cooldowns;
    }

    public ManaService mana() {
        return mana;
    }

    // ------------------------------------------------------------
    // 해금
    // ------------------------------------------------------------

    /** 지금 해금할 수 있는지. */
    public Result canUnlock(PlayerData data, SkillDefinition skill) {
        if (data.skill().unlocked().contains(skill.id())) {
            return Result.ALREADY_UNLOCKED;
        }
        if (!skill.jobId().equals(data.job().base())) {
            return Result.WRONG_JOB;
        }
        if (data.job().stage() < skill.stage().requiredJobStage()) {
            return Result.STAGE_LOCKED;
        }
        if (skill.hasParent() && !data.skill().unlocked().contains(skill.parentId())) {
            return Result.PARENT_LOCKED;
        }
        // 지시서 8장 [규칙]: 같은 분기 그룹에서 하나를 해금하면 나머지는 영구 잠금.
        for (String sibling : tree().siblingsOf(skill)) {
            if (data.skill().unlocked().contains(sibling)) {
                return Result.BRANCH_LOCKED;
            }
        }
        if (data.skill().points() < unlockCost()) {
            return Result.NOT_ENOUGH_POINTS;
        }
        return Result.OK;
    }

    /**
     * 해금 비용 (스킬 포인트).
     *
     * <p>기획서 6장은 해금을 트리로 막고 레벨을 포인트로 올리는 구조라
     * 기본값은 0이다. 운영자가 config.yml 에서 값을 줄 수 있다.
     */
    public int unlockCost() {
        return config.skill().unlockCost();
    }

    /** 스킬을 해금한다. 해금한 스킬의 레벨은 1이다. */
    public Result unlock(RpgPlayer rpgPlayer, String skillId) {
        SkillDefinition skill = tree().get(skillId);
        if (skill == null) {
            return Result.UNKNOWN_SKILL;
        }
        PlayerData data = rpgPlayer.data();
        Result check = canUnlock(data, skill);
        if (check != Result.OK) {
            return check;
        }
        data.skill().points(data.skill().points() - unlockCost());
        data.skill().unlocked().add(skillId);
        data.skill().levels().put(skillId, 1);
        saves.markDirty(data, SavePriority.IMMEDIATE);
        return Result.OK;
    }

    // ------------------------------------------------------------
    // 레벨 투자
    // ------------------------------------------------------------

    /**
     * 스킬 레벨을 올린다. 1레벨당 스킬 포인트 1개.
     *
     * @param amount 1 이상
     */
    public Result invest(RpgPlayer rpgPlayer, String skillId, int amount) {
        if (amount <= 0) {
            return Result.INVALID_AMOUNT;
        }
        SkillDefinition skill = tree().get(skillId);
        if (skill == null) {
            return Result.UNKNOWN_SKILL;
        }
        PlayerData data = rpgPlayer.data();
        if (!data.skill().unlocked().contains(skillId)) {
            return Result.NOT_UNLOCKED;
        }
        int current = data.skill().levelOf(skillId);
        if (current >= skill.maxLevel()) {
            return Result.MAX_LEVEL;
        }
        int step = Math.min(amount, skill.maxLevel() - current);
        if (data.skill().points() < step) {
            return Result.NOT_ENOUGH_POINTS;
        }
        data.skill().points(data.skill().points() - step);
        data.skill().levels().put(skillId, current + step);
        saves.markDirty(data, SavePriority.IMMEDIATE);
        return Result.OK;
    }

    // ------------------------------------------------------------
    // 발동
    // ------------------------------------------------------------

    /**
     * 스킬을 쓴다.
     *
     * <p>마나를 쓰고 쿨타임을 건 뒤 효과를 실행한다. 마나나 쿨타임에
     * 걸리면 아무 것도 소모하지 않는다.
     */
    public Result cast(RpgPlayer rpgPlayer, String skillId) {
        SkillDefinition skill = tree().get(skillId);
        if (skill == null) {
            return Result.UNKNOWN_SKILL;
        }
        PlayerData data = rpgPlayer.data();
        if (!data.skill().unlocked().contains(skillId)) {
            return Result.NOT_UNLOCKED;
        }
        if (!cooldowns.isReady(rpgPlayer, skillId)) {
            return Result.ON_COOLDOWN;
        }
        int level = data.skill().levelOf(skillId);
        double cost = skill.manaAt(level);
        if (!mana.has(rpgPlayer, cost)) {
            return Result.NOT_ENOUGH_MANA;
        }
        if (skill.effects().isEmpty()) {
            return Result.NO_EFFECT;
        }

        mana.consume(rpgPlayer, cost);
        cooldowns.start(rpgPlayer, skillId, skill.cooldownAt(level));

        SkillContext context = new SkillContext(rpgPlayer, skill, level,
                powerOf(data, skill, level), pipeline, health, players);
        boolean ran = false;
        for (SkillEffect effect : skill.effects()) {
            EffectExecutor executor = effects.get(effect.type());
            if (executor == null) {
                continue;
            }
            ran |= executor.execute(context, effect);
        }
        return ran ? Result.OK : Result.NO_EFFECT;
    }

    /** 스킬 위력 + 능력치 보정. */
    public double powerOf(PlayerData data, SkillDefinition skill, int level) {
        double power = skill.powerAt(level);
        for (Map.Entry<String, Double> entry : skill.statScaling().entrySet()) {
            power += entry.getValue() * stats.abilityPoints(data, entry.getKey());
        }
        return Math.max(0, power);
    }

    // ------------------------------------------------------------
    // 관리자
    // ------------------------------------------------------------

    /** 조건을 무시하고 해금한다. (/rpg admin skill unlock) */
    public Result forceUnlock(RpgPlayer rpgPlayer, String skillId) {
        if (!tree().has(skillId)) {
            return Result.UNKNOWN_SKILL;
        }
        PlayerData data = rpgPlayer.data();
        data.skill().unlocked().add(skillId);
        data.skill().levels().putIfAbsent(skillId, 1);
        saves.markDirty(data, SavePriority.IMMEDIATE);
        return Result.OK;
    }

    /** 해금을 지운다. (/rpg admin skill remove) */
    public Result forceRemove(RpgPlayer rpgPlayer, String skillId) {
        PlayerData data = rpgPlayer.data();
        boolean removed = data.skill().unlocked().remove(skillId);
        data.skill().levels().remove(skillId);
        // 슬롯과 조합에 남아 있으면 지운다.
        data.binding().itemSlots().replaceAll(entry -> skillId.equals(entry) ? null : entry);
        data.binding().keyCombos().removeIf(combo -> skillId.equals(combo.skillId()));
        saves.markDirty(data, SavePriority.IMMEDIATE);
        return removed ? Result.OK : Result.NOT_UNLOCKED;
    }

    /** 스킬 포인트 지급·회수. 음수면 회수한다. */
    public void addPoints(RpgPlayer rpgPlayer, int amount) {
        PlayerData data = rpgPlayer.data();
        data.skill().points(Math.max(0, data.skill().points() + amount));
        saves.markDirty(data, SavePriority.IMMEDIATE);
    }
}
