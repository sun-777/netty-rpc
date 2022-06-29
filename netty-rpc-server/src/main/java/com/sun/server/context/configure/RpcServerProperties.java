package com.sun.server.context.configure;

/**
 * @description: 自定义配置属性类
 * @author: Sun Xiaodong
 */

import com.sun.common.util.Constants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rpc")
public class RpcServerProperties {
    private NettyServer nettyServer;
    private Integer payload;
    private Integer timeout;
    private Integer maxFrameLength;


    RpcServerProperties() {
        this.maxFrameLength = Constants.DEFAULT_PAYLOAD + Constants.HEADER_LENGTH;
    }


    public NettyServer getNettyServer() {
        return nettyServer;
    }

    public void setNettyServer(NettyServer nettyServer) {
        this.nettyServer = nettyServer;
    }

    public Integer getPayload() {
        return payload;
    }

    public void setPayload(Integer payload) {
        this.payload = payload;
        setMaxFrameLength(payload + Constants.HEADER_LENGTH);
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getMaxFrameLength() {
        return maxFrameLength;
    }

    private void setMaxFrameLength(Integer maxFrameLength) {
        this.maxFrameLength = maxFrameLength;
    }

    public static final class NettyServer {
        private Integer port;
        private Integer ioThreads;
        private Integer eventLoopTimeout;


        public NettyServer() {
            this.port = Constants.DEFAULT_PORT;
            this.ioThreads = Constants.DEFAULT_IO_THREADS;
            this.eventLoopTimeout = Constants.EVENTLOOP_SHUTDOWN_TIMEOUT;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            // 校验设置的端口号是否有效: 1025 ~ 65535；0 ~ 1024为系统保留端口号
            rangeIn(port, 1025, 65535);
            this.port = port;
        }

        public Integer getIoThreads() {
            return ioThreads;
        }

        public void setIoThreads(Integer ioThreads) {
            this.ioThreads = ioThreads;
        }

        public Integer getEventLoopTimeout() {
            return eventLoopTimeout;
        }

        public void setEventLoopTimeout(Integer eventLoopTimeout) {
            this.eventLoopTimeout = eventLoopTimeout;
        }
    }


    // validate range in: [min, max]
    private static void rangeIn(final int current, int min, int max) {
        if (Math.max(0, current) != Math.min(current, max)) {
            throw new IllegalArgumentException(String.format("The given number %d can't be greater than %d or less than %d", current, max, min));
        }
    }
}
