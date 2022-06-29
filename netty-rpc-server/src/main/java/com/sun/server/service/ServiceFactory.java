package com.sun.server.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: RPC服务工厂类
 * @author: Sun Xiaodong
 */
public final class ServiceFactory {

    private final static Map<Class<?>, Object> SERVICES = new ConcurrentHashMap<>();


    // 通过RpcServer::InitRpcServiceFactory方法，利用反射机制添加服务
    @SuppressWarnings("unused")
    private static <T> void addService(Class<?> interfaceClass, T instance) {
        // 保证 instance实例类 是 interfaceClazz接口类的实现类（子类）
        if (interfaceClass.isInstance(instance)) {
            SERVICES.put(interfaceClass, instance);
        } else {
            throw new IllegalArgumentException("The instance class must be the implementation class of interfaceClass");
        }
    }

    public static Object getService(String interfaceClassName) throws ClassNotFoundException {
        Class<?> interfaceClass = Class.forName(interfaceClassName);
        return getService(interfaceClass);
    }

    public static Object getService(Class<?> interfaceClass) {
        return SERVICES.get(interfaceClass);
    }


    private ServiceFactory() {
        throw new IllegalStateException("Instantiation not allowed");
    }
}
