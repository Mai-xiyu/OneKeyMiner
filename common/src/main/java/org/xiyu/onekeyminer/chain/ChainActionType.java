package org.xiyu.onekeyminer.chain;

/**
 * 链式操作类型枚举
 * 
 * <p>定义了模组支持的所有链式操作类型，包括：</p>
 * <ul>
 *   <li>{@link #MINING} - 连锁挖掘（矿石、原木等）</li>
 *   <li>{@link #INTERACTION} - 连锁交互（剪羊毛、耕地、剥皮原木等）</li>
 *   <li>{@link #PLANTING} - 连锁种植（农作物、树苗等）</li>
 * </ul>
 * 
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.21.9
 */
public enum ChainActionType {
    
    /**
     * 连锁挖掘操作
     * 
     * <p>适用于破坏相连的同类型方块，如：</p>
     * <ul>
     *   <li>矿石采集（煤矿、铁矿、钻石矿等）</li>
     *   <li>原木砍伐（橡木、白桦木等）</li>
     *   <li>树叶清理</li>
     * </ul>
     */
    MINING("mining", "连锁挖掘"),
    
    /**
     * 连锁交互操作
     * 
     * <p>适用于对相邻目标执行右键交互，如：</p>
     * <ul>
     *   <li>剪刀剪羊毛（批量剪羊）</li>
     *   <li>锄头耕地（批量耕地）</li>
     *   <li>斧头剥皮（批量剥皮原木）</li>
     *   <li>铲子铲土（制作土径）</li>
     *   <li>刷子刷除（批量刷除可疑方块）</li>
     * </ul>
     */
    INTERACTION("interaction", "连锁交互"),
    
    /**
     * 连锁种植操作
     * 
     * <p>适用于批量种植作物和树苗，如：</p>
     * <ul>
     *   <li>小麦种子种植</li>
     *   <li>胡萝卜/土豆种植</li>
     *   <li>甜菜种子种植</li>
     *   <li>树苗种植</li>
     *   <li>模组自定义种子/作物</li>
     * </ul>
     */
    PLANTING("planting", "连锁种植"),

    /**
     * 连锁收割操作
     *
     * <p>适用于批量收割成熟作物，如：</p>
     * <ul>
     *   <li>小麦、胡萝卜、土豆、甜菜根</li>
     *   <li>下界疣</li>
     *   <li>可可豆</li>
     *   <li>甜浆果</li>
     * </ul>
     */
    HARVESTING("harvesting", "连锁收割");
    
    /** 操作类型的内部标识符 */
    private final String id;
    
    /** 操作类型的中文显示名称 */
    private final String displayName;
    
    /**
     * 构造链式操作类型
     * 
     * @param id 内部标识符
     * @param displayName 显示名称
     */
    ChainActionType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
    
    /**
     * 获取操作类型的内部标识符
     * 
     * @return 标识符字符串
     */
    public String getId() {
        return id;
    }
    
    /**
     * 获取操作类型的显示名称
     * 
     * @return 中文显示名称
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 根据标识符获取操作类型
     * 
     * @param id 标识符
     * @return 对应的操作类型，如果不存在返回 null
     */
    public static ChainActionType fromId(String id) {
        for (ChainActionType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return displayName + " (" + id + ")";
    }
}
