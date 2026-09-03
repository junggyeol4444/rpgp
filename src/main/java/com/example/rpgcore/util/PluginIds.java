package com.example.rpgcore.util;

/**
 * 지시서 1장.
 *
 * <p>플러그인 이름 / 패키지 경로 / 명령어 루트 / 권한 접두어.
 * 이 네 값은 여기서만 정의하고 나머지 코드는 전부 이 클래스를 참조한다.
 * 이름을 바꿀 때 수정 지점이 흩어지면 안 된다.
 *
 * <p>단, plugin.yml 은 리소스 파일이라 자바 상수를 참조할 수 없다.
 * 이름을 바꾸면 plugin.yml 의 name / main / commands 항목도 함께 고쳐야 한다.
 */
public final class PluginIds {

    /** 플러그인 이름. plugin.yml 의 name 과 같아야 한다. */
    public static final String PLUGIN_NAME = "RpgCore";

    /** 패키지 경로. */
    public static final String BASE_PACKAGE = "com.example.rpgcore";

    /** 명령어 루트. plugin.yml 의 commands 항목과 같아야 한다. */
    public static final String ROOT_COMMAND = "rpg";

    /** 권한 접두어. 뒤에 점이 붙어 있다. */
    public static final String PERMISSION_PREFIX = "rpgcore.";

    /** 플레이어 명령 권한 노드. 예: rpgcore.command.info */
    public static String commandPermission(String subCommand) {
        return PERMISSION_PREFIX + "command." + subCommand;
    }

    /** 관리자 명령 권한 노드. 예: rpgcore.admin.reload */
    public static String adminPermission(String subCommand) {
        return PERMISSION_PREFIX + "admin." + subCommand;
    }

    private PluginIds() {
    }
}
