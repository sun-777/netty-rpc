package com.sun.client.proxy;

import org.springframework.beans.factory.FactoryBean;

import javax.annotation.Resource;

/**
 * @description:
 * @author: Sun Xiaodong
 */
public class RpcFactoryBean<T> implements FactoryBean<T> {
    private final Class<T> interfaceClass;

    @Resource
    private RpcProxy rpcProxy;

    public RpcFactoryBean(final Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getObject() {
        return (T)rpcProxy.getProxy(interfaceClass);
    }

    @Override
    public Class<T> getObjectType() {
        return this.interfaceClass;
    }
}
