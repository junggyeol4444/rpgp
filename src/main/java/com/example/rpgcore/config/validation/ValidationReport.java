package com.example.rpgcore.config.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 지시서 6장.
 *
 * <p>설정 로드 시 잘못된 항목을 모아두는 리포트.
 * 잘못된 항목은 해당 항목만 건너뛰고 파일명·경로·이유를 남긴다.
 * 서버를 죽이지 않는다.
 */
public final class ValidationReport {

    /** 한 건의 문제. */
    public record Entry(Severity severity, String file, String path, String reason) {

        @Override
        public String toString() {
            return "[" + severity + "] " + file + " : " + path + " - " + reason;
        }
    }

    public enum Severity {
        /** 항목을 건너뛰었다. */
        ERROR,
        /** 기본값으로 대체했거나 무시해도 되는 문제. */
        WARN
    }

    private final List<Entry> entries = new ArrayList<>();

    public void error(String file, String path, String reason) {
        entries.add(new Entry(Severity.ERROR, file, path, reason));
    }

    public void warn(String file, String path, String reason) {
        entries.add(new Entry(Severity.WARN, file, path, reason));
    }

    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public int errorCount() {
        int n = 0;
        for (Entry e : entries) {
            if (e.severity() == Severity.ERROR) {
                n++;
            }
        }
        return n;
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void clear() {
        entries.clear();
    }
}
