package com.sun.server.handler;

import com.sun.common.enumerator.Event;
import com.sun.common.exchange.message.Request;
import com.sun.common.exchange.message.RequestBody;
import com.sun.common.exchange.message.RequestHeader;
import com.sun.common.exchange.message.Response;
import com.sun.common.exchange.message.ResponseBody;
import com.sun.common.exchange.message.ResponseHeader;
import com.sun.common.exchange.message.ResponseStatus;
import com.sun.server.context.configure.RpcServerProperties;
import com.sun.server.service.ServiceFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.reflect.FastClass;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @description: RPC业务服务端处理器
 * @author: Sun Xiaodong
 */

public class RpcServerHandler extends SimpleChannelInboundHandler<Request> {
    private static final Logger log = LoggerFactory.getLogger(RpcServerHandler.class);

    private final RpcServerProperties properties;
    // 业务线程池
    private final EventExecutorGroup group;

    public RpcServerHandler(final EventExecutorGroup group, RpcServerProperties properties) {
        this.group = group;
        this.properties = properties;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("client[{}] connected.", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("client[{}] disconnected.", ctx.channel().remoteAddress());
        ctx.channel().close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request msg) {
        final RequestHeader requestHeader = msg.getHeader();
        final String requestId = requestHeader.getId().toString();
        log.info("Received {} {}",  (requestHeader.getEvent() == Event.HEARTBEAT) ? "heartbeat" : "request", requestId);
        // 响应请求（异步执行 + 超时处理）
        if (requestHeader.getResponseRequired()) {
            group.submit(() -> {
                // 客户端超时时间
                final long clientTimeout = requestHeader.getTimeoutMillis();
                // 服务端超时时间
                final long serverTimeout = properties.getTimeout();

                // 0 表示没有超时限制，如果客户端没有超时限制，那么以服务端超时设置为准；
                // 如果客户端有超时，服务端也有超时，那么以超时时间短的为准；
                final long timeout = 0 == clientTimeout ? serverTimeout : Math.min(clientTimeout, serverTimeout);
                final boolean isServerTimeout = (0 == clientTimeout) || (clientTimeout >= serverTimeout);

                final ResponseHeader responseHeader = (ResponseHeader) ResponseHeader.getDefault()
                                                                                     .setId(requestHeader.getId())
                                                                                     .setEvent(requestHeader.getEvent())
                                                                                     .setSerialization(requestHeader.getSerialization());
                final Response response = new Response().setHeader(responseHeader);
                CompletableFuture<Response> completableFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        Object result = handle(msg);
                        responseHeader.setStatus(ResponseStatus.OK);
                        response.setBody(new ResponseBody(result));
                    } catch (Exception e) {
                        response.setBody(new ResponseBody(e.getCause(), e.getMessage()));
                        if (e instanceof ClassNotFoundException) {
                            responseHeader.setStatus(ResponseStatus.SERVICE_NOT_FOUND);
                        } else {
                            responseHeader.setStatus(ResponseStatus.SERVICE_ERROR);
                        }
                        e.printStackTrace();
                    }
                    return response;
                });

                try {
                    if (0 == timeout) {
                        completableFuture.get();
                    } else {
                        completableFuture.get(timeout, TimeUnit.MILLISECONDS);
                    }
                } catch (Exception e) {
                    response.setBody(new ResponseBody(e.getCause(), e.getMessage()));
                    if (e instanceof TimeoutException) { // 超时
                        responseHeader.setStatus(isServerTimeout ? ResponseStatus.SERVER_TIMEOUT : ResponseStatus.CLIENT_TIMEOUT);
                    } else {
                        responseHeader.setStatus(ResponseStatus.SERVER_ERROR);
                    }
                }
                sendResponse(ctx.channel(), response);
            });
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 当Channel已经断开的情况下, 仍然发送数据, 会抛异常, 该方法会被调用.
        cause.printStackTrace();
        ctx.channel().close();
        super.exceptionCaught(ctx, cause);
    }


    private void sendResponse(final Channel channel, final Response response) {
        channel.eventLoop().submit(() -> channel.writeAndFlush(response).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("Send response for request {}", response.getHeader().getId().toString());
            } else {
                Throwable cause = future.cause();
                if (Objects.nonNull(cause)) {
                    cause.printStackTrace();
                }
            }})
        );
    }


    private Object handle(Request request) throws ClassNotFoundException, InvocationTargetException {
        final RequestBody body = request.getBody();
        final String interfaceName = body.getInterfaceName();
        Object serviceBean = ServiceFactory.getService(interfaceName);
        if (Objects.isNull(serviceBean)) {
            log.error("Can not find service implement with interface: {}", interfaceName);
            throw new ClassNotFoundException("Can not find service implement with interface: " + interfaceName);
        }
        final Class<?> serviceBeanClass = serviceBean.getClass();
        // JDK reflect
        //Method method = serviceBeanClass.getMethod(body.getMethodName(), body.getParameterTypes());
        //method.setAccessible(true);
        //return method.invoke(serviceBean, body.getParameters());

        // Cglib reflect
        final FastClass serviceFastClass = FastClass.create(serviceBeanClass);
        final int methodIndex = serviceFastClass.getIndex(body.getMethodName(), body.getParameterTypes());
        return serviceFastClass.invoke(methodIndex, serviceBean, body.getParameters());
    }
}
