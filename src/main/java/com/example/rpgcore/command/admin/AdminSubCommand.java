package com.example.rpgcore.command.admin;

import com.example.rpgcore.command.SubCommand;
import com.example.rpgcore.util.PluginIds;

/**
 * /rpg admin 아래에 붙는 관리자 명령.
 *
 * <p>지시서 12장 [권한 노드]: 관리자 명령은 rpgcore.admin.&lt;하위명령&gt;
 * 노드를 쓴다.
 */
public interface AdminSubCommand extends SubCommand {

    @Override
    default String permission() {
        return PluginIds.adminPermission(name());
    }

    @Override
    default String usage() {
        String hint = argHint();
        return "/" + PluginIds.ROOT_COMMAND + " admin " + name()
                + (hint.isEmpty() ? "" : " " + hint);
    }

    /** 도움말에 붙일 인자 설명. 예: {@code <player> <level>} */
    default String argHint() {
        return "";
    }
}
