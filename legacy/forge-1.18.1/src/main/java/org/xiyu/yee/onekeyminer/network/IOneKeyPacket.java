package org.xiyu.yee.onekeyminer.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * 所有一键连锁网络包的基础接口（简化为不继承 CustomPacketPayload，以兼容 1.20.1）
 */
public interface IOneKeyPacket {
    /**
     * 将数据写入缓冲区
     */
    void write(FriendlyByteBuf buf);
    
    /**
     * 获取数据包ID
     */
    ResourceLocation id();

    /**
     * 返回绑定的 packet 类型占位（兼容层）
     */
    default Object type() { return null; }
}