package com.sun.client.handler;

import com.sun.client.context.RpcClient;
import com.sun.common.enumerator.Event;
import com.sun.common.exchange.message.Request;
import com.sun.common.exchange.message.RequestBody;
import com.sun.common.exchange.message.RequestHeader;
import com.sun.common.service.HeartbeatService;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * @description: 心跳事件客户端处理器
 *               当客户端触发了写空闲事件，则向服务器发送心跳信息
 * @author: Sun Xiaodong
 */
public class HeartBeatClientHandler extends ChannelDuplexHandler {
    private static final Logger log = LoggerFactory.getLogger(HeartBeatClientHandler.class);

    private final RpcClient client;

    public HeartBeatClientHandler(RpcClient client) {
        this.client = client;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        final IdleStateEvent event = (IdleStateEvent) evt;
        if (IdleState.ALL_IDLE == event.state()) {
            // 发送心跳请求
            ctx.channel().eventLoop().execute(() -> {
                RequestHeader header = (RequestHeader) RequestHeader.getDefault(client.properties.getTimeout())
                        .setResponseRequired(false)
                        .setEvent(Event.HEARTBEAT)
                        .setSerialization(client.properties.getSerializer());
                try {
                    final Class<?> heartbeatServiceClass = HeartbeatService.class;
                    final RequestBody body = new RequestBody().setInterfaceName(heartbeatServiceClass.getName());
                    Method method = heartbeatServiceClass.getDeclaredMethod("ping", new Class<?>[0]);
                    body.setMethodName(method.getName())
                        .setReturnType(method.getReturnType())
                        .setParameterTypes(method.getParameterTypes())
                        .setParameters(method.getParameters());
                    // 发送ping消息
                    client.sendRequest0(new Request(header, body));
                } catch (NoSuchMethodException e) {
                    log.error("{}", e);
                }
            });
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
