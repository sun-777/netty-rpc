package com.sun.server.context;

import com.sun.common.annotation.RpcService;
import com.sun.common.exchange.codec.ExchangeCodec;
import com.sun.common.netty.NettyOperation;
import com.sun.common.netty.codec.CodecAdapter;
import com.sun.common.netty.codec.RpcProtocolFrameDecoder;
import com.sun.common.util.Constants;
import com.sun.server.context.configure.RpcServerProperties;
import com.sun.server.handler.HeartBeatServerHandler;
import com.sun.server.handler.RpcServerHandler;
import com.sun.server.service.ServiceFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @description: RPC服务
 * @author: Sun Xiaodong
 */


@Component
public class RpcServer implements ApplicationContextAware, InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(RpcServer.class);

    @Resource
    private RpcServerProperties properties;

    private NettyServer server;

    @Override
    public void afterPropertiesSet() {
        start();
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        try {
            InitRpcServiceFactory(applicationContext);
        } catch (NoSuchMethodException e) {
            log.error("{}", e);
        }
    }


    private void InitRpcServiceFactory(final ApplicationContext applicationContext) throws NoSuchMethodException {
        // 反射调用ServicesFactory.addService 私有成员方法
        final Method addServiceMethod = ServiceFactory.class.getDeclaredMethod("addService", Class.class, Object.class);
        addServiceMethod.setAccessible(true);
        // k: 服务接口实现类的全限定类名; v: 服务接口实现类bean对象
        applicationContext.getBeansWithAnnotation(RpcService.class).forEach((k, v) -> {
            // 根据bean对象，获取它的接口类
            Class<?>[] interfaceClasses = v.getClass().getInterfaces();
            try {
                for (Class<?> interfaceClass : interfaceClasses) {
                    // 添加服务接口类的全限定名、及其对应的实现类bean对象到ServiceFactory
                    addServiceMethod.invoke(null, interfaceClass, v);
                }
            } catch (ReflectiveOperationException e) {
                log.error("{}", e);
            }
        });
    }


    private void start() {
        this.server = new NettyServer();
        this.server.setName("netty_server");
        this.server.start();
    }


    @PreDestroy
    public void close() {
        server.doClose();
    }


    class NettyServer extends Thread implements NettyOperation {
        private static final String BOOS_EVENTLOOP_POOL_NAME = "server_boss_eventloop";
        private static final String WORKER_EVENTLOOP_POOL_NAME = "server_worker_eventloop";
        private static final String HANDLER_THREAD_POOL_NAME = "rpc_handler_eventloop";

        private Channel channel;
        private EventLoopGroup boss;
        private EventLoopGroup worker;
        // Rpc业务线程池
        private EventExecutorGroup handlerGroup;

        public NettyServer() {}


        @Override
        public void run() {
            doOpen();
        }

        @Override
        public void doOpen() {
            try {
                ServerBootstrap bootstrap = initServerBootstrap();
                // 绑定端口，开启监听
                ChannelFuture future = Objects.requireNonNull(bootstrap).bind(getBindAddress()).syncUninterruptibly();
                this.channel = future.channel();
                //future.channel().closeFuture().syncUninterruptibly();
            } catch (Exception e) {
                log.error("{}", e);
            }
        }


        @Override
        public void doClose() {
            try {
                if (Objects.nonNull(channel) && channel.isOpen()) {
                    channel.close();
                }
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }

            final long eventLoopTimeout = properties.getNettyServer().getEventLoopTimeout();
            final long quietPeriod = Math.min(Constants.EVENTLOOP_SHUTDOWN_QUIET_PERIOD, eventLoopTimeout);
            try {
                if (Objects.nonNull(this.boss)) {
                    this.boss.shutdownGracefully(quietPeriod, eventLoopTimeout, TimeUnit.MILLISECONDS).syncUninterruptibly();
                }
            } catch (Exception e) {
                log.warn("{}", e);
            }
            try {
                if (Objects.nonNull(this.worker)) {
                    this.worker.shutdownGracefully(quietPeriod, eventLoopTimeout, TimeUnit.MILLISECONDS).syncUninterruptibly();
                }
            } catch (Exception e) {
                log.warn("{}", e);
            }
            try {
                this.handlerGroup.shutdownGracefully(quietPeriod, eventLoopTimeout, TimeUnit.MILLISECONDS).syncUninterruptibly();
            } catch (Exception e) {
                log.warn("{}", e);
            }
            log.info("Netty server closed.");
        }


        private ServerBootstrap initServerBootstrap() {
            try {
                this.boss = NettyOperation.eventLoopGroup(1, BOOS_EVENTLOOP_POOL_NAME);
                this.worker = NettyOperation.eventLoopGroup(properties.getNettyServer().getIoThreads(), WORKER_EVENTLOOP_POOL_NAME);
                this.handlerGroup = NettyOperation.eventLoopGroup(16, HANDLER_THREAD_POOL_NAME);
                return new ServerBootstrap().group(boss, worker)
                      .option(ChannelOption.SO_REUSEADDR, true)
                      .childOption(ChannelOption.TCP_NODELAY, true)
                      .childOption(ChannelOption.SO_KEEPALIVE, true)
                      .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                      // 调整netty的接收缓冲区大小
                      //.childOption(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator())
                      //.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                      .channel(NettyOperation.serverSocketChannelClass())
                      .childHandler(new ChannelInitializer<SocketChannel>() {
                          @Override
                          protected void initChannel(SocketChannel ch) {
                              final ChannelPipeline pipeline = ch.pipeline();
                              CodecAdapter adapter = new CodecAdapter(new ExchangeCodec());
                              pipeline.addLast("idle_state_handler", new IdleStateHandler(0, 0, 5, TimeUnit.SECONDS))
                                      .addLast("heartbeat_handler", new HeartBeatServerHandler())
                                      .addLast("rpc_frame_decoder", new RpcProtocolFrameDecoder(properties.getMaxFrameLength()))
                                      .addLast("rpc_decoder", adapter.getDecoder())
                                      .addLast("rpc_encoder", adapter.getEncoder())
                                      .addLast("rpc_business_server_handler", new RpcServerHandler(handlerGroup, RpcServer.this.properties));
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
                if (Objects.nonNull(boss)) {
                    try {
                        boss.shutdownGracefully();
                    } catch (Exception exception) {
                        log.warn("{}", exception);
                    }
                }
                if (Objects.nonNull(worker)) {
                    try {
                        worker.shutdownGracefully();
                    } catch (Exception exception) {
                        log.warn("{}", exception);
                    }
                }
                return null;
            }
        }


        private InetSocketAddress getBindAddress() throws UnknownHostException {
            final int port = properties.getNettyServer().getPort();
            return new InetSocketAddress(InetAddress.getLocalHost(), port);
        }
    }
}
