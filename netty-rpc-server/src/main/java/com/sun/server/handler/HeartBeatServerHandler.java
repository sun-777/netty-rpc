package com.sun.server.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @description: 心跳事件服务端处理器
 *               客户端的写空闲时间、服务端的读空闲时间必需合理设置，否则会让服务端误判客户端已经掉线，从而导致客户端强制下线。
 *               一般 服务端的读空闲时间 是 客户端的写空闲时间 的2~3倍，即为合理
 * @author: Sun Xiaodong
 */
public class HeartBeatServerHandler extends ChannelDuplexHandler {
    private static final Logger log = LoggerFactory.getLogger(HeartBeatServerHandler.class);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        final IdleStateEvent event = (IdleStateEvent) evt;
        // 是否触发了服务端的读写空闲事件，有触发，说明客户端掉线了
        if (IdleState.ALL_IDLE == event.state()) {
            log.info("No heartbeat from client[{}]", ctx.channel().remoteAddress());
            ctx.channel().close();
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
