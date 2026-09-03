package com.example.rpgcore.stat;

import java.util.EnumMap;
import java.util.Map;

/**
 * 지시서 3장 [stat/DerivedStats] — 능력치를 환산한 결과.
 *
 * <p>불변이다. 스탯·직업·스킬이 바뀔 때만 다시 만들고, 그 사이에는
 * {@code RpgPlayer} 가 들고 있는 것을 그대로 읽는다.
 * 데미지 이벤트는 호출 빈도가 높아 매번 계산하면 안 된다. (지시서 11장)
 */
public final class DerivedStats {

    private final Map<DerivedStat, Double> values;

    private DerivedStats(Map<DerivedStat, Double> values) {
        this.values = values;
    }

    /** 전부 0인 결과. 설정을 읽지 못했을 때 쓴다. */
    public static DerivedStats empty() {
        return new DerivedStats(new EnumMap<>(DerivedStat.class));
    }

    public static Builder builder() {
        return new Builder();
    }

    public double get(DerivedStat stat) {
        Double value = values.get(stat);
        return value == null ? 0.0 : value;
    }

    /** 표시용. 순서는 열거형 선언 순서를 따른다. */
    public Map<DerivedStat, Double> asMap() {
        return new EnumMap<>(values);
    }

    /** 값을 쌓아 올린다. */
    public static final class Builder {

        private final Map<DerivedStat, Double> values = new EnumMap<>(DerivedStat.class);

        public Builder add(DerivedStat stat, double amount) {
            if (amount == 0.0) {
                return this;
            }
            values.merge(stat, amount, Double::sum);
            return this;
        }

        public Builder set(DerivedStat stat, double amount) {
            values.put(stat, amount);
            return this;
        }

        public double get(DerivedStat stat) {
            Double value = values.get(stat);
            return value == null ? 0.0 : value;
        }

        public DerivedStats build() {
            return new DerivedStats(new EnumMap<>(values));
        }
    }
}
