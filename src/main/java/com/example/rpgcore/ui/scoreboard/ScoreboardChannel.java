package com.example.rpgcore.ui.scoreboard;

import com.example.rpgcore.level.CombatLevelService;
import com.example.rpgcore.player.RpgPlayer;
import com.example.rpgcore.ui.HudChannel;
import com.example.rpgcore.util.Messages;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * 지시서 10장 — 스코어보드에 레벨 · 경험치.
 *
 * <p>[확인 필요 - 지시서 16장]
 * 스코어보드 API({@code getScoreboardManager}, {@code registerNewObjective},
 * {@code DisplaySlot})가 26.x 에서 유효한지 확인한다.
 * 스코어보드를 만지는 곳은 이 파일뿐이며, config.yml 의
 * ui.channels.scoreboard 로 끌 수 있다.
 */
public final class ScoreboardChannel implements HudChannel {

    private static final String OBJECTIVE_NAME = "rpgcore";

    private final Messages messages;
    private final CombatLevelService levels;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private final Map<UUID, List<String>> shownLines = new HashMap<>();

    public ScoreboardChannel(Messages messages, CombatLevelService levels) {
        this.messages = messages;
        this.levels = levels;
    }

    @Override
    public String id() {
        return "scoreboard";
    }

    @Override
    public void attach(RpgPlayer rpgPlayer) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective(
                OBJECTIVE_NAME, "dummy", messages.format("hud.scoreboard.title"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        boards.put(rpgPlayer.uuid(), board);
        shownLines.put(rpgPlayer.uuid(), new ArrayList<>());
        rpgPlayer.player().setScoreboard(board);
    }

    @Override
    public void update(RpgPlayer rpgPlayer) {
        Scoreboard board = boards.get(rpgPlayer.uuid());
        if (board == null) {
            attach(rpgPlayer);
            board = boards.get(rpgPlayer.uuid());
        }
        Objective objective = board.getObjective(OBJECTIVE_NAME);
        if (objective == null) {
            return;
        }

        List<String> lines = buildLines(rpgPlayer);
        List<String> previous = shownLines.get(rpgPlayer.uuid());
        if (lines.equals(previous)) {
            return;
        }
        for (String line : previous) {
            board.resetScores(line);
        }
        // 위에서부터 읽히도록 점수를 거꾸로 매긴다.
        int score = lines.size();
        for (String line : lines) {
            objective.getScore(line).setScore(score--);
        }
        shownLines.put(rpgPlayer.uuid(), lines);
    }

    @Override
    public void detach(RpgPlayer rpgPlayer) {
        boards.remove(rpgPlayer.uuid());
        shownLines.remove(rpgPlayer.uuid());
    }

    private List<String> buildLines(RpgPlayer rpgPlayer) {
        int level = rpgPlayer.data().combat().level();
        double exp = rpgPlayer.data().combat().exp();
        List<String> lines = new ArrayList<>();
        lines.add(messages.format("hud.scoreboard.level", "level", level));
        if (levels.isMaxLevel(rpgPlayer.data())) {
            lines.add(messages.format("hud.scoreboard.exp-max",
                    "exp", CombatLevelService.format(exp)));
        } else {
            lines.add(messages.format("hud.scoreboard.exp",
                    "exp", CombatLevelService.format(exp),
                    "required", CombatLevelService.format(levels.requiredExp(level))));
        }
        return lines;
    }
}
