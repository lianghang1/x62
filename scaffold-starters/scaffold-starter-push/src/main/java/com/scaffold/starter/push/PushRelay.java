package com.scaffold.starter.push;

/**
 * 跨实例转发 SPI：多实例部署时把推送消息广播到所有节点。
 *
 * <p>默认空实现（单实例，本机直接投递）；开启
 * {@code scaffold.push.multi-instance=true} 后由 Redis pub/sub 实现——
 * 每个节点订阅同一频道，收到后只在本节点投递，横向扩容对业务透明。</p>
 *
 * @author lianghang
 */
public interface PushRelay {

    /**
     * 把消息扇出到全部节点（含本节点；本节点通过订阅回调投递）。
     *
     * @param message 消息信封
     */
    void relay(PushMessage message);
}
