package com.sun.common.id.serialization;

/**
 * @description: 序列化器工厂类
 * @author: Sun Xiaodong
 */
public interface SerializerFactory<T extends Serializer> {
    T newSerializer();
}
