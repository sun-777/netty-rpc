package com.sun.server.service.impl;

import com.sun.common.service.HelloService;
import com.sun.common.annotation.RpcService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @description:
 * @author: Sun Xiaodong
 */

@RpcService
public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello(String name) {
        return "Hello, ".concat(name);
    }


    @Override
    public List<Integer> random() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        final List<Integer> list = new ArrayList<>();
        for (int i = 0, size = 10; i < size; i++) {
            list.add(random.nextInt(999));
        }
        return list;
    }
}
