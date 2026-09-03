package com.example.rpgcore.region;

/**
 * 지시서 8장 [regions.yml] — 지역 하나.
 *
 * <p>기획서 4장: 몬스터를 플레이어 레벨에 맞춰 스케일링하지 않고,
 * 지역별로 고정 레벨대를 두고 상위 지역을 계속 추가한다.
 *
 * <p>범위는 x · z 평면만 본다. 높이는 보지 않는다.
 *
 * @param id       지역 id
 * @param display  표시 이름
 * @param world    월드 이름
 * @param minX     범위. x1 · x2 중 작은 값
 * @param minZ     범위. z1 · z2 중 작은 값
 * @param maxX     범위. x1 · x2 중 큰 값
 * @param maxZ     범위. z1 · z2 중 큰 값
 * @param minLevel 레벨대 하한
 * @param maxLevel 레벨대 상한
 */
public record RegionDefinition(String id, String display, String world,
                               double minX, double minZ, double maxX, double maxZ,
                               int minLevel, int maxLevel) {

    /** 설정에 적힌 두 점을 최소·최대로 정리해서 만든다. */
    public static RegionDefinition of(String id, String display, String world,
                                      double x1, double z1, double x2, double z2,
                                      int minLevel, int maxLevel) {
        return new RegionDefinition(id, display, world,
                Math.min(x1, x2), Math.min(z1, z2),
                Math.max(x1, x2), Math.max(z1, z2),
                Math.min(minLevel, maxLevel), Math.max(minLevel, maxLevel));
    }

    /** 좌표가 이 지역 안인지. 경계선도 포함한다. */
    public boolean contains(String worldName, double x, double z) {
        return world.equals(worldName)
                && x >= minX && x <= maxX
                && z >= minZ && z <= maxZ;
    }

    /** 넓이. 겹치는 지역 중 좁은 쪽을 고를 때 쓴다. */
    public double area() {
        return (maxX - minX) * (maxZ - minZ);
    }
}
