package com.example.rpgcore.core;

import com.example.rpgcore.config.validation.ValidationReport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 지시서 3장.
 *
 * <p>서비스 접근 지점. 서비스는 여기에만 등록하고, 서로를 직접
 * new 하지 않는다.
 *
 * <p>등록 순서를 그대로 기억해 enable 은 등록 순서대로,
 * disable 은 역순으로 호출한다.
 */
public final class ServiceRegistry {

    private final Map<Class<?>, Object> services = new LinkedHashMap<>();
    private final List<Lifecycle> lifecycles = new ArrayList<>();
    private final List<Reloadable> reloadables = new ArrayList<>();

    /**
     * 서비스를 등록한다. 같은 타입을 두 번 등록하면 예외를 던진다.
     * (부팅 시점 실수를 조용히 넘기지 않기 위한 것이다.)
     */
    public <T> T register(Class<T> type, T instance) {
        if (services.containsKey(type)) {
            throw new IllegalStateException("서비스가 이미 등록되었습니다: " + type.getName());
        }
        services.put(type, instance);
        if (instance instanceof Lifecycle lifecycle) {
            lifecycles.add(lifecycle);
        }
        if (instance instanceof Reloadable reloadable) {
            reloadables.add(reloadable);
        }
        return instance;
    }

    /** 등록된 서비스를 꺼낸다. 없으면 예외를 던진다. */
    public <T> T get(Class<T> type) {
        Object found = services.get(type);
        if (found == null) {
            throw new IllegalStateException("서비스가 등록되지 않았습니다: " + type.getName());
        }
        return type.cast(found);
    }

    /** 등록된 서비스를 꺼낸다. 없으면 null. */
    public <T> T find(Class<T> type) {
        Object found = services.get(type);
        return found == null ? null : type.cast(found);
    }

    /** 등록 순서대로 enable 한다. */
    public void enableAll() {
        for (Lifecycle lifecycle : lifecycles) {
            lifecycle.enable();
        }
    }

    /** 등록 역순으로 disable 한다. 한 서비스가 실패해도 나머지는 계속 내린다. */
    public List<Throwable> disableAll() {
        List<Throwable> failures = new ArrayList<>();
        for (int i = lifecycles.size() - 1; i >= 0; i--) {
            try {
                lifecycles.get(i).disable();
            } catch (RuntimeException | Error e) {
                failures.add(e);
            }
        }
        return failures;
    }

    /** 등록 순서대로 reload 한다. */
    public void reloadAll(ValidationReport report) {
        for (Reloadable reloadable : reloadables) {
            reloadable.reload(report);
        }
    }

    public List<Lifecycle> lifecycles() {
        return List.copyOf(lifecycles);
    }
}
