package org.xiyu.onekeyminer.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Path;

/**
 * 平台服务抽象层
 * 
 * <p>定义了跨平台的服务接口，用于处理 Fabric、NeoForge 和 Forge 之间的差异。
 * 各平台需要在入口点初始化时调用 {@link #setInstance(PlatformServices)} 注册实现。</p>
 * 
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.21.9
 */
public interface PlatformServices {
    
    /** 平台服务实例 */
    Holder HOLDER = new Holder();
    
    /**
     * 获取平台服务实例
     * 
     * @return 平台服务实例
     * @throws RuntimeException 如果实例未初始化
     */
    static PlatformServices getInstance() {
        PlatformServices instance = HOLDER.instance;
        if (instance == null) {
            throw new RuntimeException("PlatformServices 尚未初始化！请确保平台入口点已调用 setInstance()。");
        }
        return instance;
    }
    
    /**
     * 设置平台服务实例（由各平台入口点调用）
     * 
     * @param instance 平台服务实现
     */
    static void setInstance(PlatformServices instance) {
        HOLDER.instance = instance;
    }
    
    /**
     * 检查服务是否已初始化
     * 
     * @return 如果已初始化返回 true
     */
    static boolean isInitialized() {
        return HOLDER.instance != null;
    }
    
    /** 用于存储实例的持有者类 */
    class Holder {
        PlatformServices instance;
    }
    
    // ========== 便捷访问器（兼容旧代码） ==========
    
    /** @deprecated 使用 {@link #getInstance()} 代替 */
    @Deprecated
    PlatformServices INSTANCE = null; // 保留字段以兼容编译，但不使用
    
    /**
     * 获取当前运行的平台名称
     * 
     * @return 平台名称（"fabric", "neoforge", "forge"）
     */
    String getPlatformName();
    
    /**
     * 判断当前是否为客户端环境
     * 
     * @return 如果是客户端环境返回 true
     */
    boolean isClient();
    
    /**
     * 判断当前是否为服务端环境
     * 
     * @return 如果是服务端环境返回 true
     */
    boolean isDedicatedServer();
    
    /**
     * 获取配置文件目录
     * 
     * @return 配置文件目录路径
     */
    Path getConfigDirectory();
    
    /**
     * 检查玩家是否有权限在指定位置破坏方块
     * 
     * <p>该方法用于与保护插件（如 FTB Chunks、Residence）集成。
     * 各平台应实现对应的权限检查逻辑。</p>
     * 
     * @param player 服务端玩家
     * @param level 世界实例
     * @param pos 方块位置
     * @param state 方块状态
     * @return 如果玩家有权限破坏该方块返回 true
     */
    boolean canPlayerBreakBlock(ServerPlayer player, Level level, BlockPos pos, BlockState state);
    
    /**
     * 检查玩家是否有权限在指定位置交互
     * 
     * @param player 服务端玩家
     * @param level 世界实例
     * @param pos 方块位置
     * @param state 方块状态
     * @return 如果玩家有权限交互返回 true
     */
    default boolean canPlayerInteract(ServerPlayer player, Level level, BlockPos pos, BlockState state) {
        // 默认使用破坏权限检查
        return canPlayerBreakBlock(player, level, pos, state);
    }
    
    /**
     * 模拟玩家破坏方块
     * 
     * <p><strong>关键方法</strong>：此方法必须正确模拟玩家破坏方块的行为，
     * 确保以下机制正常工作：</p>
     * <ul>
     *   <li>触发 BlockBreakEvent（保护插件兼容）</li>
     *   <li>正确应用战利品表</li>
     *   <li>正确处理工具耐久度和附魔效果</li>
     *   <li>播放破坏音效和粒子效果</li>
     * </ul>
     * 
     * <p><strong>禁止</strong>使用 {@code world.setBlock(pos, Blocks.AIR.defaultBlockState())}，
     * 这会绕过所有原版机制。</p>
     * 
     * @param player 服务端玩家
     * @param level 世界实例
     * @param pos 方块位置
     * @return 如果方块成功被破坏返回 true
     */
    boolean simulateBlockBreak(ServerPlayer player, Level level, BlockPos pos);
    
    /**
     * 模拟玩家对方块使用物品
     * 
     * <p>用于连锁交互功能（耕地、剥皮等）。
     * 必须触发相应的交互事件。</p>
     * 
     * @param player 服务端玩家
     * @param level 世界实例
     * @param pos 方块位置
     * @param hand 使用的手
     * @param item 使用的物品
     * @return 如果交互成功返回 true
     */
    default boolean simulateItemUseOnBlock(
            ServerPlayer player, 
            Level level, 
            BlockPos pos, 
            InteractionHand hand, 
            ItemStack item
    ) {
        // 默认实现：由 ChainActionLogic 处理
        return false;
    }
    
    /**
     * 检查指定模组是否已加载
     * 
     * @param modId 模组 ID
     * @return 如果模组已加载返回 true
     */
    boolean isModLoaded(String modId);
    
    /**
     * 注册配置界面工厂
     * 
     * <p>由各平台在模组初始化时调用，用于注册原生配置 GUI。</p>
     */
    void registerConfigScreen();
    
    /**
     * 检查玩家的链式模式是否激活
     * 
     * @param player 玩家
     * @return 如果链式模式激活返回 true
     */
    default boolean isChainModeActive(ServerPlayer player) {
        // 由各平台实现，可能使用 Capability 或 DataAttachment
        return true;
    }
    
    /**
     * 设置玩家的链式模式状态
     * 
     * @param player 玩家
     * @param active 是否激活
     */
    default void setChainModeActive(ServerPlayer player, boolean active) {
        // 由各平台实现
    }
    
    /**
     * 发送触觉反馈（如手柄震动）
     * 
     * @param player 目标玩家
     * @param intensity 震动强度（0.0 - 1.0）
     * @param duration 持续时间（tick）
     */
    default void sendHapticFeedback(ServerPlayer player, float intensity, int duration) {
        // 默认空实现，各平台可选择性覆盖
    }
    
    /**
     * 向玩家发送链式操作完成的消息
     * 
     * @param player 玩家
     * @param actionType 操作类型 ID
     * @param count 操作数量
     */
    default void sendChainActionMessage(ServerPlayer player, String actionType, int count) {
        // 由各平台实现网络包发送
    }
}
