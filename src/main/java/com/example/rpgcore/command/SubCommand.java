package com.example.rpgcore.command;

import com.example.rpgcore.util.PluginIds;
import java.util.List;
import org.bukkit.command.CommandSender;

/**
 * 지시서 12장 — /rpg 하위 명령 하나.
 *
 * <p>명령어는 plugin.yml 에 /rpg 하나만 등록하고, 하위 명령은
 * 코드에서 분기한다. (지시서 2장)
 */
public interface SubCommand {

    /** 하위 명령 이름. 소문자로 둔다. */
    String name();

    /** 필요한 권한 노드. */
    String permission();

    /** 도움말에 쓸 messages.yml 경로. */
    String descriptionKey();

    /** 콘솔에서 쓸 수 없는 명령인지. */
    default boolean playerOnly() {
        return false;
    }

    /** 도움말에 표시할 사용법. */
    default String usage() {
        return "/" + PluginIds.ROOT_COMMAND + " " + name();
    }

    /**
     * 실행한다.
     *
     * @param args 하위 명령 이름을 뺀 나머지 인자
     */
    void execute(CommandSender sender, String[] args);

    /** 탭 완성 후보. */
    default List<String> complete(CommandSender sender, String[] args) {
        return List.of();
    }
}
