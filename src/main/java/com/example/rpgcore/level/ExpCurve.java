package com.example.rpgcore.level;

import com.example.rpgcore.config.schema.CurveSettings;
import com.example.rpgcore.config.validation.ValidationReport;

/**
 * 지시서 3장 [level/ExpCurve] — 요구 경험치 계산.
 *
 * <p>기획서 3장: 레벨 상한이 없고 요구 경험치는 지수형이다.
 * 반환값이 long 범위를 넘어서므로 double 을 쓴다.
 */
public interface ExpCurve {

    /**
     * 지정한 레벨에서 다음 레벨로 가는 데 필요한 경험치.
     *
     * @param level 현재 레벨 (1 이상)
     */
    double requiredExp(int level);

    /** 어떤 곡선인지. 설정의 type 값과 같다. */
    String type();

    /**
     * 설정에서 곡선을 만든다.
     *
     * <p>알 수 없는 type 이면 리포트에 남기고 지수형 기본값으로 대체한다.
     * (지시서 6장: 서버를 죽이지 않는다)
     *
     * @param settings 설정값
     * @param fallback type 이 잘못됐을 때 쓸 기본 설정
     * @param file     리포트에 남길 파일명
     * @param path     리포트에 남길 경로
     * @param report   리포트. null 허용
     */
    static ExpCurve from(CurveSettings settings, CurveSettings fallback,
                         String file, String path, ValidationReport report) {
        CurveSettings target = settings;
        if (target == null) {
            target = fallback;
        }
        if (!CurveSettings.EXPONENTIAL.equalsIgnoreCase(target.type())) {
            if (report != null) {
                report.error(file, path + ".type",
                        "알 수 없는 곡선 종류라 " + CurveSettings.EXPONENTIAL
                                + " 기본값으로 대체합니다: " + target.type());
            }
            target = fallback;
        }
        if (target.base() <= 0 || target.factor() <= 0) {
            if (report != null) {
                report.error(file, path,
                        "base 와 factor 는 0보다 커야 합니다. 기본값으로 대체합니다. base="
                                + target.base() + ", factor=" + target.factor());
            }
            target = fallback;
        }
        return new Exponential(target.base(), target.factor());
    }

    /**
     * 지수형 곡선. required(level) = base * factor^(level - 1)
     */
    final class Exponential implements ExpCurve {

        private final double base;
        private final double factor;

        public Exponential(double base, double factor) {
            this.base = base;
            this.factor = factor;
        }

        @Override
        public double requiredExp(int level) {
            int effective = Math.max(level, 1);
            return base * Math.pow(factor, effective - 1);
        }

        @Override
        public String type() {
            return CurveSettings.EXPONENTIAL;
        }

        public double base() {
            return base;
        }

        public double factor() {
            return factor;
        }
    }
}
