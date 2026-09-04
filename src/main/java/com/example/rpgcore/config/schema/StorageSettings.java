package com.example.rpgcore.config.schema;

/**
 * 지시서 8장 / config.yml 의 storage 블록.
 *
 * @param type                     저장소 구현 이름. 현재 YAML 만 있다.
 * @param autoSaveIntervalSeconds  지연 저장(주기 저장) 간격. 기본 300초
 * @param ioThreads                저장·로드에 쓸 스레드 수
 */
public record StorageSettings(String type, int autoSaveIntervalSeconds, int ioThreads) {

    public static final String YAML = "YAML";

    public static StorageSettings defaults() {
        return new StorageSettings(YAML, 300, 2);
    }
}
