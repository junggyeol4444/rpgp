package com.example.rpgcore.util;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;

/**
 * 지시서 6장: 모든 출력 문구는 messages.yml 에서 읽는다.
 * 코드에 문구를 박지 않는다.
 *
 * <p>치환자는 {@code {이름}} 형식이며, {@code format("a.b", "level", 3)}
 * 처럼 키와 값을 번갈아 넘긴다.
 *
 * <p>[확인 완료] {@code CommandSender#sendMessage(String)} 는 26.1.2 에
 * 있고 사용 중단이 아니다. Paper 26.1.2 API 소스로 컴파일해 확인했다. (tools/verify-against-paper.sh)
 * 색 코드 처리는 아직 붙이지 않았다. 문구에 색 코드를 쓰지 않는다.
 * 메시지를 보내는 API 는 이 클래스 한 곳에만 있다.
 */
public final class Messages {

    private final Logger logger;
    private volatile Map<String, String> values = new LinkedHashMap<>();
    private final Set<String> reportedMissing = new HashSet<>();

    public Messages(Logger logger) {
        this.logger = logger;
    }

    /** messages.yml 을 다 읽은 뒤 통째로 갈아끼운다. */
    public void replaceAll(Map<String, String> loaded) {
        this.values = new LinkedHashMap<>(loaded);
        synchronized (reportedMissing) {
            reportedMissing.clear();
        }
    }

    /** 접두어. messages.yml 의 prefix. */
    public String prefix() {
        String prefix = values.get("prefix");
        return prefix == null ? "" : prefix;
    }

    /**
     * 문구를 찾아 치환자를 채운다.
     *
     * @param path        messages.yml 의 경로 (예: {@code common.no-permission})
     * @param replacements 치환자 이름과 값을 번갈아 넘긴다
     */
    public String format(String path, Object... replacements) {
        String template = values.get(path);
        if (template == null) {
            reportMissing(path);
            return "!" + path;
        }
        if (replacements.length == 0) {
            return template;
        }
        if (replacements.length % 2 != 0) {
            logger.warning("치환자 개수가 짝이 맞지 않습니다: " + path);
            return template;
        }
        String result = template;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            String key = "{" + replacements[i] + "}";
            String value = String.valueOf(replacements[i + 1]);
            result = result.replace(key, value);
        }
        return result;
    }

    /** 접두어를 붙여 보낸다. */
    public void send(CommandSender target, String path, Object... replacements) {
        target.sendMessage(prefix() + format(path, replacements));
    }

    /** 접두어 없이 보낸다. 여러 줄로 늘어놓는 화면에 쓴다. */
    public void sendPlain(CommandSender target, String path, Object... replacements) {
        target.sendMessage(format(path, replacements));
    }

    /** 문구가 등록되어 있는지. */
    public boolean has(String path) {
        return values.containsKey(path);
    }

    public int size() {
        return values.size();
    }

    private void reportMissing(String path) {
        synchronized (reportedMissing) {
            if (reportedMissing.add(path)) {
                logger.warning("messages.yml 에 문구가 없습니다: " + path);
            }
        }
    }
}
