package com.scaffold.server.lite.cemlink.park;

/**
 * 园区定制策略：按 park-code 一单位一策略，通用行为用 default 兜底，差异点逐个覆写
 *
 * <p>扩展约定：新增领域差异点 = 在本接口加 default 方法（通用实现），
 * 单位策略类按需覆写，不新增 Bean；{@code CemlinkConfig} 的 {@code @Import}
 * 列表只随单位数增长</p>
 *
 * @author lianghang
 */
public interface ParkStrategy {

    /** 墓穴路径清洗，通用：原样返回 */
    default String cleanTombPath(String tombPath) {
        return tombPath;
    }

    /** 墓穴层号清洗，通用：原样返回 */
    default String cleanLayerCode(String layerCode) {
        return layerCode;
    }

    /** 顶层虚拟墓区，通用：无则 null */
    default SuperBlock superBlock() {
        return null;
    }

    /**
     * 顶层虚拟墓区（原程序硬编码 id=20967「卫家角」）
     */
    class SuperBlock {

        private final String id;
        private final String blockCode;
        private final String blockName;
        private final String isBottom;
        private final Integer lengthGrid;
        private final Integer widthGrid;

        public SuperBlock(String id, String blockCode, String blockName,
                          String isBottom, Integer lengthGrid, Integer widthGrid) {
            this.id = id;
            this.blockCode = blockCode;
            this.blockName = blockName;
            this.isBottom = isBottom;
            this.lengthGrid = lengthGrid;
            this.widthGrid = widthGrid;
        }

        public String getId() {
            return id;
        }

        public String getBlockCode() {
            return blockCode;
        }

        public String getBlockName() {
            return blockName;
        }

        public String getIsBottom() {
            return isBottom;
        }

        public Integer getLengthGrid() {
            return lengthGrid;
        }

        public Integer getWidthGrid() {
            return widthGrid;
        }
    }
}
