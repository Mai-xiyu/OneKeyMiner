package org.xiyu.onekeyminer.shape;

import net.minecraft.resources.ResourceLocation;
import org.xiyu.onekeyminer.OneKeyMiner;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 形状注册表
 * 
 * <p>管理所有已注册的连锁搜索形状。内置形状在模组初始化时注册，
 * 附属模组可以在任何时候通过 {@link #register(ChainShape)} 添加自定义形状。</p>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.0
 * @since Minecraft 1.20.1
 */
public final class ShapeRegistry {
    
    /** 已注册形状映射 (ID -> 形状实例) */
    private static final Map<ResourceLocation, ChainShape> SHAPES = new ConcurrentHashMap<>();
    
    /** 注册顺序列表（保证配置界面中的循环顺序稳定） */
    private static final List<ResourceLocation> SHAPE_ORDER = Collections.synchronizedList(new ArrayList<>());
    
    /** 默认形状 ID */
    public static final ResourceLocation DEFAULT_SHAPE_ID = new ResourceLocation(OneKeyMiner.MOD_ID, "amorphous");
    
    private ShapeRegistry() {
        // 工具类，禁止实例化
    }
    
    /**
     * 注册一个连锁搜索形状
     * 
     * <p>如果已存在相同 ID 的形状，将被覆盖并记录警告。</p>
     * 
     * @param shape 形状实例
     * @throws NullPointerException 如果 shape 或其 ID 为 null
     */
    public static void register(ChainShape shape) {
        Objects.requireNonNull(shape, "Shape must not be null");
        Objects.requireNonNull(shape.getId(), "Shape ID must not be null");
        
        ResourceLocation id = shape.getId();
        if (SHAPES.containsKey(id)) {
            OneKeyMiner.LOGGER.warn("形状 {} 已被覆盖注册", id);
            // 不重复添加到顺序列表
        } else {
            SHAPE_ORDER.add(id);
        }
        SHAPES.put(id, shape);
        OneKeyMiner.LOGGER.debug("已注册形状: {} ({})", id, shape.getTranslationKey());
    }
    
    /**
     * 获取指定 ID 的形状
     * 
     * @param id 形状 ID
     * @return 形状实例，如果不存在返回 null
     */
    public static ChainShape getShape(ResourceLocation id) {
        return SHAPES.get(id);
    }
    
    /**
     * 获取指定 ID 的形状，如果不存在返回默认形状
     * 
     * @param id 形状 ID
     * @return 形状实例
     */
    public static ChainShape getShapeOrDefault(ResourceLocation id) {
        ChainShape shape = SHAPES.get(id);
        if (shape == null) {
            shape = SHAPES.get(DEFAULT_SHAPE_ID);
        }
        if (shape == null && !SHAPES.isEmpty()) {
            // 兜底：返回第一个注册的形状
            shape = SHAPES.values().iterator().next();
        }
        return shape;
    }
    
    /**
     * 通过字符串 ID 获取形状
     * 
     * @param idStr 形状 ID 字符串，格式 "namespace:path"
     * @return 形状实例，如果不存在或格式错误返回默认形状
     */
    public static ChainShape getShapeOrDefault(String idStr) {
        try {
            ResourceLocation id = new ResourceLocation(idStr);
            return getShapeOrDefault(id);
        } catch (Exception e) {
            OneKeyMiner.LOGGER.warn("无效的形状 ID: {}，使用默认形状", idStr);
            return getShapeOrDefault(DEFAULT_SHAPE_ID);
        }
    }
    
    /**
     * 检查形状 ID 是否已注册
     * 
     * @param id 形状 ID
     * @return 如果已注册返回 true
     */
    public static boolean isRegistered(ResourceLocation id) {
        return SHAPES.containsKey(id);
    }
    
    /**
     * 检查字符串形状 ID 是否有效
     * 
     * @param idStr 形状 ID 字符串
     * @return 如果有效且已注册返回 true
     */
    public static boolean isValidShapeId(String idStr) {
        try {
            ResourceLocation id = new ResourceLocation(idStr);
            return SHAPES.containsKey(id);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取所有已注册的形状（按注册顺序）
     * 
     * @return 有序的形状列表（不可变）
     */
    public static List<ChainShape> getAllShapes() {
        List<ChainShape> result = new ArrayList<>();
        for (ResourceLocation id : SHAPE_ORDER) {
            ChainShape shape = SHAPES.get(id);
            if (shape != null) {
                result.add(shape);
            }
        }
        return Collections.unmodifiableList(result);
    }
    
    /**
     * 获取所有已注册的形状 ID（按注册顺序）
     * 
     * @return 有序的 ID 列表（不可变）
     */
    public static List<ResourceLocation> getAllShapeIds() {
        return Collections.unmodifiableList(new ArrayList<>(SHAPE_ORDER));
    }
    
    /**
     * 获取已注册形状数量
     * 
     * @return 形状数量
     */
    public static int getShapeCount() {
        return SHAPES.size();
    }
    
    /**
     * 获取下一个形状 ID（用于配置界面循环切换）
     * 
     * @param currentId 当前形状 ID 字符串
     * @return 下一个形状 ID 字符串
     */
    public static String getNextShapeId(String currentId) {
        List<ResourceLocation> ids = getAllShapeIds();
        if (ids.isEmpty()) return DEFAULT_SHAPE_ID.toString();
        
        for (int i = 0; i < ids.size(); i++) {
            if (ids.get(i).toString().equals(currentId)) {
                return ids.get((i + 1) % ids.size()).toString();
            }
        }
        return ids.get(0).toString();
    }
}
