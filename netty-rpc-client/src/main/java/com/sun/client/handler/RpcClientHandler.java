package com.sun.client.handler;

import com.sun.common.exchange.message.Response;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @description: RPC业客户端处理器
 * @author: Sun Xiaodong
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<Response> {
    //private static final Logger log = LoggerFactory.getLogger(RpcClientHandler.class);

    private final Map<String, CompletableFuture<Response>> responseFutures;
    // 业务线程池
    private final EventExecutorGroup group;

    public RpcClientHandler(EventExecutorGroup group, Map<String, CompletableFuture<Response>> responseFutures) {
        this.group = group;
        this.responseFutures = responseFutures;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Response res) throws Exception {
        // 复制一份，res在方法内的代码执行完时被释放
        final Response response = res.copy();
        group.submit(() -> {
            final String requestId = response.getHeader().getId().toString();
            final CompletableFuture<Response> future = responseFutures.get(requestId);
            // 已经完成 或者 调用complete方法返回false时，移除此future对象
            // 未完成时，调用complete方法且返回true时，说明future对象已正确写入结果response
            if(future.isDone() || !future.complete(response)) {
                responseFutures.remove(requestId);
            }
        });
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 当Channel已经断开的情况下, 仍然发送数据, 会抛异常, 该方法会被调用.
        cause.printStackTrace();
        ctx.close();
    }
}
