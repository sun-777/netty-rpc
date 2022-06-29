package com.sun.client.context;

import com.sun.client.context.configure.RpcClientProperties;
import com.sun.client.handler.HeartBeatClientHandler;
import com.sun.client.handler.RpcClientHandler;
import com.sun.common.enumerator.Event;
import com.sun.common.exchange.codec.ExchangeCodec;
import com.sun.common.exchange.message.Request;
import com.sun.common.exchange.message.Response;
import com.sun.common.netty.NettyOperation;
import com.sun.common.netty.codec.CodecAdapter;
import com.sun.common.netty.codec.RpcProtocolFrameDecoder;
import com.sun.common.util.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @description: RPC 客户端
 * @author: Sun Xiaodong
 */

@Component
public class RpcClient {
    private static final Logger log = LoggerFactory.getLogger(RpcClient.class);

    @Resource
    public RpcClientProperties properties;

    // RPC客户端异步请求时，缓存sendRequest中生成的CompletableFuture<Response>对象，
    private final Map<String, CompletableFuture<Response>> responseFutures;

    private NettyClient client;

    public RpcClient() {
        responseFutures = new ConcurrentHashMap<>();
    }


    @PostConstruct
    public void init() {
        startNettyClient();
    }


    private void startNettyClient() {
        this.client = new NettyClient();
        this.client.setName("rpc_client");
        this.client.start();
    }


    @PreDestroy
    public void close() {
        this.client.doClose();
    }


    // 成功响应，则通过requestId移除对应的CompleteFuture对象
    public void removeCompleteFutureObject(final String requestId) {
        responseFutures.remove(requestId);
    }

    // 超时或出错时，调用CompleteFuture对象的Complete方法，将其状态置为完成
    public void completeCompleteFutureObject(final String requestId) {
        CompletableFuture<Response> future = responseFutures.get(requestId);
        // 已经完成 或者 调用complete方法返回false时，移除此future对象
        if(future.isDone() || !future.complete(Response.VOID)) {
            responseFutures.remove(requestId);
        }
    }


    // 发送请求
    public void sendRequest0(Request request) {
        try {
            final String requestId = request.getHeader().getId().toString();
            final boolean isHeartbeat = request.getHeader().getEvent() == Event.HEARTBEAT;
            final Channel channel = client.getChannel();
            if (channel.isActive()) {
                channel.eventLoop().submit(() ->
                channel.writeAndFlush(request).addListener((ChannelFutureListener) channelFuture -> {
                    if (channelFuture.isSuccess()) {
                        log.info("Send {}: {}", isHeartbeat ? "heartbeat" : "request", requestId);
                    } else {
                        Throwable cause = channelFuture.cause();
                        if (Objects.nonNull(cause)) {
                            cause.printStackTrace();
                        }
                    }})
                );
            } else {
                log.error("channel is disconnected");
            }
        } catch (Exception e) {
            log.error("{}", e);
        }
    }

    // 发送请求且返回一个CompletableFuture<Response>对象
    public CompletableFuture<Response> sendRequest(Request request) {
        final String requestId = request.getHeader().getId().toString();
        CompletableFuture<Response> future = new CompletableFuture<>();
        responseFutures.put(requestId, future);
        sendRequest0(request);
        return future;
    }



    class NettyClient extends Thread implements NettyOperation {
        private static final String EVENT_LOOP_POOL_NAME = "client_event_loop";
        private static final String HANDLER_THREAD_POOL_NAME = "rpc_handler_event_loop";

        private volatile Channel channel;
        private EventLoopGroup group;
        // Rpc业务线程池
        private EventExecutorGroup handlerGroup;

        public NettyClient() {}

        public Channel getChannel() {
            return this.channel;
        }


        @Override
        public void run() {
            doOpen();
        }


        @Override
        public void doOpen() {
            try {
                Bootstrap bootstrap = initBootstrap();
                this.channel = Objects.requireNonNull(bootstrap).connect(getConnectAddress()).syncUninterruptibly().channel();
            } catch (Exception e) {
                log.error("{}", e);
            }
        }


        @Override
        public void doClose() {
            try {
                if (Objects.nonNull(this.channel) && this.channel.isOpen()) {
                    this.channel.close();
                }
            } catch (Exception e) {
                log.warn("{}", e);
            }

            final long timeoutMillis = properties.getNettyClient().getTimeout();
            final long quietPeriod = Math.min(Constants.EVENTLOOP_SHUTDOWN_QUIET_PERIOD, timeoutMillis);
            try {
                if (Objects.nonNull(handlerGroup)) {
                    handlerGroup.shutdownGracefully(quietPeriod, timeoutMillis, TimeUnit.MILLISECONDS).syncUninterruptibly();
                }
            } catch (Exception exception) {
                log.warn("{}", exception);
            }
            try {
                if (Objects.nonNull(this.group)) {
                    this.group.shutdownGracefully(quietPeriod, timeoutMillis, TimeUnit.MILLISECONDS).syncUninterruptibly();
                }
            } catch (Exception exception) {
                log.warn("{}", exception);
            }
            log.info("Netty client closed.");
        }


        private Bootstrap initBootstrap() {
            try {
                this.group = NettyOperation.eventLoopGroup(1, EVENT_LOOP_POOL_NAME);
                this.handlerGroup = NettyOperation.eventLoopGroup(16, HANDLER_THREAD_POOL_NAME);
                return new Bootstrap().group(group)
                      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getNettyClient().getConnectionTimeout())
                      .channel(NettyOperation.socketChannelClass())
                      .handler(new ChannelInitializer<SocketChannel>() {
                          @Override
                          protected void initChannel(SocketChannel ch) {
                              final ChannelPipeline pipeline = ch.pipeline();
                              CodecAdapter adapter = new CodecAdapter(new ExchangeCodec());
                              pipeline.addLast("idle_state_handler", new IdleStateHandler(0, 0, 2, TimeUnit.SECONDS))
                                      .addLast("heartbeat_handler", new HeartBeatClientHandler(RpcClient.this))
                                      .addLast("rpc_frame_decoder", new RpcProtocolFrameDecoder(properties.getMaxFrameLength()))
                                      .addLast("rpc_decoder", adapter.getDecoder())
                                      .addLast("rpc_encoder", adapter.getEncoder())
                                      .addLast("rpc_business_client_handler", new RpcClientHandler(handlerGroup, responseFutures));
                          }
                      });
            } catch (Exception e) {
                log.error("{}", e);
                if (Objects.nonNull(handlerGroup)) {
                    try {
                        handlerGroup.shutdownGracefully();
                    } catch (Exception exception) {
                        log.warn("{}", exception);
                    }
                }
                if (Objects.nonNull(group)) {
                    try {
                        group.shutdownGracefully();
                    } catch (Exception exception) {
                        log.warn("{}", exception);
                    }
                }
                return null;
            }
        }


        private InetSocketAddress getConnectAddress() {
            final String ip = properties.getNettyClient().getAddress();
            final int port = properties.getNettyClient().getPort();
            return new InetSocketAddress(ip, port);
        }
    }
}
