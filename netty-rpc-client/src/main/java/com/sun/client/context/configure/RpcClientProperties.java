package com.sun.client.context.configure;

import com.sun.common.enumerator.CodeKeyEnum;
import com.sun.common.enumerator.Serialization;
import com.sun.common.util.Constants;
import com.sun.common.util.StringUtil;
import com.sun.common.util.validator.InetAddressValidator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Objects;

/**
 * @description:
 * @author: Sun Xiaodong
 */

@Configuration
@ConfigurationProperties(prefix = "rpc")
public class RpcClientProperties {
    private NettyClient nettyClient;
    private Integer payload;
    private Integer timeout;
    private Serialization serializer;
    private Integer maxFrameLength;


    RpcClientProperties() {
        this.maxFrameLength = Constants.DEFAULT_PAYLOAD + Constants.HEADER_LENGTH;
        timeout = 0;
    }


    public NettyClient getNettyClient() {
        return nettyClient;
    }

    public void setNettyClient(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
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

    public Serialization getSerializer() {
        return serializer;
    }

    public void setSerializer(String serializer) {
        Serialization serializerEnum = findEnum(Serialization.values(), serializer);
        if (Objects.isNull(serializerEnum)) {
            throw new IllegalArgumentException("Serializer is not a valid serialization enum name");
        }
        this.serializer = serializerEnum;
    }


    public Integer getMaxFrameLength() {
        return maxFrameLength;
    }

    private void setMaxFrameLength(Integer maxFrameLength) {
        this.maxFrameLength = maxFrameLength;
    }



    public static final class NettyClient {
        private String address;
        private Integer port;
        private Integer timeout;
        private Integer connectionTimeout;

        public NettyClient() {
            this.port = Constants.DEFAULT_PORT;
            this.timeout = Constants.EVENTLOOP_SHUTDOWN_TIMEOUT;
            this.connectionTimeout = Constants.CONNECTION_TIMEOUT;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            // 如果不是ip形式，且不是"localhost"，那么address是hostname
            // 需要验证hostname是否有效（即：在hosts文件中是否存在此hostname）
            address = StringUtil.strip(address);
            if (!InetAddressValidator.getInstance().isValid(address) && !address.toLowerCase(Locale.ENGLISH).contains(Constants.LOCALHOST)) {
                if (!validHostnameFromHosts(address)) {
                    throw new IllegalArgumentException("Unknown address, please set address correctly");
                }
            }
            this.address = address;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            // 校验设置的端口号是否有效: 1025 ~ 65535；0 ~ 1024为系统保留端口号
            rangeIn(port, 1025, 65535);
            this.port = port;
        }

        public Integer getTimeout() {
            return timeout;
        }

        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }

        public Integer getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(Integer connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }
    }


    /**
     * 根据hostname从hosts文件中查找对应的ip
     * @param hostname
     * @return
     */
    private static boolean validHostnameFromHosts(final String hostname) {
        final File file = new File(getHostsPath());
        if (file.isFile()) {
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));) {
                String line;
                while (null != (line = reader.readLine())) {
                    line = StringUtil.strip(line);
                    final int offset = line.indexOf(hostname);
                    if (offset > 0) {
                        // hosts文件中，ip 与 hostname映射格式如下：
                        // 140.82.114.4    github.com
                        // 127.0.0.1 localhost
                        // ::1         localhost localhost.localdomain localhost6 localhost6.localdomain6
                        // 故，只需从行首开始，读到空格字符则截断
                        int i = 0;
                        for (; i < offset; i++) {
                            final char ch = line.charAt(i);
                            if (Character.isWhitespace(ch)) {
                                String ip = line.substring(0, i);
                                return InetAddressValidator.getInstance().isValid(ip);
                            }
                        }
                    }
                }
            } catch (IOException e) {
            }
        }
        return false;
    }


    // 找hosts文件路径
    private static String getHostsPath() {
        // “Windows”, “Mac”, “Unix” and “Solaris”
        final String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        String hosts = Constants.HOSTS_DEFAULT;
        if (os.contains(Constants.OS_WIN_PREFIX)) {
            hosts = Constants.HOSTS_WINDOWS;
        }
        return hosts;
    }


    // 根据给定的enumString（不区分大小写），查找枚举接口类CodeKeyEnum的子类的枚举对象
    private static <T extends Enum<T> & CodeKeyEnum<T, C, K>, C extends Number, K> T findEnum(final T[] enumValues, String enumName) {
        T enumObject = null;
        if (enumValues.length > 0 && !StringUtil.isBlank(enumName)) {
            enumName = StringUtil.strip(enumName);
            for (T value : enumValues) {
                if (value.name().equalsIgnoreCase(enumName)) {
                    enumObject = value;
                    break;
                }
            }
        }
        return enumObject;
    }


    // validate range in: [min, max]
    private static void rangeIn(final int current, int min, int max) {
        if (Math.max(0, current) != Math.min(current, max)) {
            throw new IllegalArgumentException(String.format("The given number %d can't be greater than %d or less than %d", current, max, min));
        }
    }
}
