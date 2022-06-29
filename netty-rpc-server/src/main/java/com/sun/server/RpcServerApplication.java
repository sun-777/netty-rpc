package com.sun.server;

import com.sun.common.util.Constants;
import com.sun.server.annotation.RpcServiceScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.time.ZoneId;
import java.util.TimeZone;

// 扫描所有@RpcService注解的Rpc服务实现类
@RpcServiceScan(basePackages = {"com.sun.server.service.impl"})
@SpringBootApplication
public class RpcServerApplication {

    @PostConstruct
    public void setTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(Constants.DEFAULT_TIME_ZONE)));
    }

    public static void main(String[] args) {
        //System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(RpcServerApplication.class, args);
    }
}
