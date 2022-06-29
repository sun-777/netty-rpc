package com.sun.common.service;

import com.sun.common.annotation.RpcServiceInterface;

import java.util.List;

/**
 * @description: 测试接口
 * @author: Sun Xiaodong
 */

@RpcServiceInterface
public interface HelloService {
    String sayHello(String name);
    List<Integer> random();
}
