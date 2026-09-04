package com.example.rpgcore.util;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** 명령 처리에서 반복되는 잔손질. */
public final class CommandUtil {

    private CommandUtil() {
    }

    /**
     * 접속 중인 플레이어를 이름으로 찾는다. 없으면 null.
     *
     * <p>TODO 오프라인 플레이어 대상 조작은 저장소를 비동기로 읽어야 한다.
     *      1단계에서는 접속 중인 플레이어만 다룬다.
     */
    public static Player onlinePlayer(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return Bukkit.getPlayerExact(name);
    }

    /**
     * 비동기 작업이 끝난 뒤 결과를 보낼 때 쓴다.
     * 그 사이 명령을 친 플레이어가 나갔으면 보내지 않는다.
     */
    public static void safeSend(Messages messages, CommandSender sender,
                                String path, Object... replacements) {
        if (sender instanceof Player player && !player.isOnline()) {
            return;
        }
        messages.send(sender, path, replacements);
    }

    /** 정수 파싱. 실패하면 null. */
    public static Integer parseInt(String raw) {
        try {
            return Integer.valueOf(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 실수 파싱. 실패하거나 유한하지 않으면 null. */
    public static Double parseDouble(String raw) {
        try {
            double value = Double.parseDouble(raw);
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
