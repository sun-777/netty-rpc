package com.sun.common.netty;

import com.sun.common.util.Constants;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.Locale;
import java.util.concurrent.ThreadFactory;

/**
 * @description: Netty Server, Client接口类
 * @author: Sun Xiaodong
 */
public interface NettyOperation {

    void doOpen();
    void doClose();


    static EventLoopGroup eventLoopGroup(final int threads, final String threadFactoryName) {
        ThreadFactory threadFactory = new DefaultThreadFactory(threadFactoryName, false);
        return shouldEpoll() ? new EpollEventLoopGroup(threads, threadFactory) : new NioEventLoopGroup(threads, threadFactory);
    }


    static Class<? extends SocketChannel> socketChannelClass() {
        return shouldEpoll() ? EpollSocketChannel.class : NioSocketChannel.class;
    }


    static Class<? extends ServerSocketChannel> serverSocketChannelClass() {
        return shouldEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
    }

    // MacOS 使用的是Kqueue，Linux/Unix使用的是Epoll，Windows使用的是IOCP
    static boolean shouldEpoll() {
        if (Boolean.parseBoolean(System.getProperty(Constants.NETTY_EPOLL_ENABLE_KEY, "false"))) {
            // “Windows”, “Mac”, “Unix” and “Solaris”
            final String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
            final boolean epollSupportPlatform = os.contains(Constants.OS_LINUX_PREFIX) || os.contains(Constants.OS_UNIX) || os.contains(Constants.OS_SOLARIS);
            return epollSupportPlatform && Epoll.isAvailable();
        }
        return false;
    }

}