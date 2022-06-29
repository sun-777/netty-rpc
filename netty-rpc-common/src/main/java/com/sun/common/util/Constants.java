package com.sun.common.util;

import com.sun.common.exchange.codec.ExchangeCodec;

/**
 * @description:
 * @author: Sun Xiaodong
 */
public interface Constants {

    String DEFAULT_TIME_ZONE = "Asia/Shanghai";

    String PING = "ping";
    String PONG = "pong";

    // OS prefix
    String OS_WIN_PREFIX = "win";
    String OS_LINUX_PREFIX = "linux";
    String OS_UNIX = "unix";
    String OS_SOLARIS = "solaris";
    String OS_MAC = "mac";

    String LOCALHOST = "localhost";
    String HOSTS_WINDOWS = "C:/Windows/System32/drivers/etc/hosts";
    String HOSTS_DEFAULT = "/etc/hosts";


    int  HEADER_LENGTH = ExchangeCodec.HEADER_LENGTH;

    // RPC通信协议中HEADER中表示body内容长度的字段偏移量
    int BODY_OFFSET = ExchangeCodec.BODY_LENGTH_OFFSET;
    int BODY_LENGTH = ExchangeCodec.BODY_LENGTH_LENGTH;
    // RPC通信协议支持的最大package
    int DEFAULT_PAYLOAD = 0x100000;  // 默认payload大小是1M



    String NETTY_EPOLL_ENABLE_KEY = "netty.epoll.enable";
    // Netty io threads number
    int DEFAULT_IO_THREADS = Math.min(Runtime.getRuntime().availableProcessors() + 1, 32);
    // Netty基于长度的变长帧解码器最大长度
    int DEFAULT_MAX_FRAME_LENGTH = HEADER_LENGTH + DEFAULT_PAYLOAD;
    // Netty Server默认的监听端口号
    int DEFAULT_PORT = 20820;
    // 关闭Netty event loop线程池时默认静默时间，单位：ms
    long EVENTLOOP_SHUTDOWN_QUIET_PERIOD = 2000L;
    // 关闭Netty event loop线程池时超时时间，单位：ms
    int EVENTLOOP_SHUTDOWN_TIMEOUT = 3000;
    // Netty client连接超时时间
    int CONNECTION_TIMEOUT = 3000;

}
