package com.example.rpgcore.region;

import com.example.rpgcore.config.ConfigManager;
import com.example.rpgcore.core.Lifecycle;
import java.util.Collection;
import org.bukkit.Location;

/**
 * 지시서 3장 [region/RegionService] — 지역별 레벨대.
 *
 * <p>지시서 14장 [병행 작업]: 지역은 2단계 이후 필요 시점에 붙인다.
 * 퀘스트의 REACH 목표가 지역을 쓰기 때문에 5단계에서 들어왔다.
 *
 * <p>정의는 ConfigManager 가 읽고, 여기서는 찾기만 한다.
 */
public final class RegionService implements Lifecycle {

    private final ConfigManager config;

    public RegionService(ConfigManager config) {
        this.config = config;
    }

    @Override
    public String serviceName() {
        return "RegionService";
    }

    public Collection<RegionDefinition> all() {
        return config.regions().values();
    }

    /** 없으면 null. */
    public RegionDefinition byId(String id) {
        return id == null ? null : config.regions().get(id);
    }

    /**
     * 좌표가 속한 지역. 겹치면 좁은 쪽을 고른다.
     * 겹치는 지역을 둘 때 상세 지역이 이기도록 하기 위해서다.
     */
    public RegionDefinition regionAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        String world = location.getWorld().getName();
        double x = location.getX();
        double z = location.getZ();

        RegionDefinition best = null;
        for (RegionDefinition region : all()) {
            if (!region.contains(world, x, z)) {
                continue;
            }
            if (best == null || region.area() < best.area()) {
                best = region;
            }
        }
        return best;
    }
}
