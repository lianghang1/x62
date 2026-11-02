package com.scaffold.cemlink.park;

/**
 * 墓区园区定制策略：顶层虚拟墓区等园区差异的覆盖点
 *
 * @author lianghang
 */
public interface BlockParkStrategy {

    /**
     * 顶层虚拟墓区（原程序硬编码 id=20967「卫家角」），无则返回 null
     */
    SuperBlock superBlock();

    /**
     * 顶层虚拟墓区描述
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
