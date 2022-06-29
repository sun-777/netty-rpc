package com.sun.client;


import com.sun.client.annotation.RpcServiceInterfaceScan;
import com.sun.common.util.Constants;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.time.ZoneId;
import java.util.TimeZone;

// 扫描所有@RpcServiceInterface注解的RPC服务接口类
@RpcServiceInterfaceScan(basePackages = {"com.sun.common.service"})
@SpringBootApplication
public class RpcClientApplication {

    @PostConstruct
    public void setTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(Constants.DEFAULT_TIME_ZONE)));
    }

    public static void main(String[] args) {
        SpringApplication.run(RpcClientApplication.class, args);
    }
}
