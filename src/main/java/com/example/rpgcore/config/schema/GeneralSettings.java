package com.example.rpgcore.config.schema;

/**
 * config.yml 의 파싱 결과.
 *
 * @param storage 저장소 설정
 * @param debug   디버그 모드 초기값. /rpg admin debug 로 런타임 토글된다.
 */
public record GeneralSettings(StorageSettings storage, boolean debug) {

    public static GeneralSettings defaults() {
        return new GeneralSettings(StorageSettings.defaults(), false);
    }
}
