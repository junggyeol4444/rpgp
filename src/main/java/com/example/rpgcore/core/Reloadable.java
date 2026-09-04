package com.example.rpgcore.core;

import com.example.rpgcore.config.validation.ValidationReport;

/**
 * 지시서 3장.
 *
 * <p>/rpg admin reload 로 다시 읽어들일 대상이 구현한다.
 * 구현체는 잘못된 값을 만나면 예외를 던지지 말고 리포트에 남기고
 * 해당 항목만 건너뛴다. (지시서 6장)
 */
public interface Reloadable {

    /**
     * 설정을 다시 읽는다.
     *
     * @param report 문제를 기록할 리포트
     */
    void reload(ValidationReport report);
}
