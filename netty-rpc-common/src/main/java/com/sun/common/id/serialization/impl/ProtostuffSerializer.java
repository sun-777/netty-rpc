package com.sun.common.id.serialization.impl;

import com.sun.common.id.serialization.Serializer;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: 通过Protostuff实现序列化，反序列化
 * @author: Sun Xiaodong
 */
public final class ProtostuffSerializer implements Serializer {

    // 缓存Schema
    private static final Map<Class<?>, WeakReference<Object>> SCHEMA_CACHE = new ConcurrentHashMap<>();


    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T> T deserialize(Class<T> clazz, byte[] data) throws RuntimeException {
        try {
            if (shouldWrapper(clazz)) {
                Schema<SerializationWrapper> schema = (Schema<SerializationWrapper>) getSchema(SerializationWrapper.class).get();
                SerializationWrapper<T> wrapperObj = schema.newMessage();
                ProtostuffIOUtil.mergeFrom(data, wrapperObj, schema);
                return wrapperObj.getData();
            } else {
                Schema<T> schema = (Schema<T>) getSchema(clazz).get();
                T t = schema.newMessage();
                ProtostuffIOUtil.mergeFrom(data, t, schema);
                return t;
            }
        } finally {
            // 防止reference弱引用对象在使用过程中被GC回收，需要JDK 9+支持
            //Reference.reachabilityFence(this);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T> byte[] serialize(T obj) throws RuntimeException {
        try {
            final LinkedBuffer buffer = getBuffer();
            Class<T> clazz = (Class<T>) obj.getClass();
            if (shouldWrapper(clazz)) {
                SerializationWrapper<T> wrapperObj = SerializationWrapper.builder(obj);
                WeakReference<Object> reference = getSchema(SerializationWrapper.class);
                return ProtostuffIOUtil.toByteArray(wrapperObj, (Schema<SerializationWrapper>) reference.get(), buffer);
            } else {
                WeakReference<Object> reference = getSchema(clazz);
                return ProtostuffIOUtil.toByteArray(obj, (Schema<T>) reference.get(), buffer);
            }
        } finally {
            // 防止reference弱引用对象在使用过程中被GC回收，需要JDK 9+支持
            //Reference.reachabilityFence(this);
        }
    }


    // 是否需要包装（当遇到Protostuff不支持序列化/反序列化数组、集合类等对象时，需要使用包装类包装）
    private static <T> boolean shouldWrapper(Class<T> clazz) {
        return null != clazz && (clazz.isArray() || Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz));
    }


    private static LinkedBuffer getBuffer() {
        return LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
    }


    private static <T> WeakReference<Object> getSchema(Class<T> clazz) {
        WeakReference<Object> reference = SCHEMA_CACHE.get(clazz);
        if (Objects.isNull(reference)) {
            Schema<T> schema = RuntimeSchema.getSchema(clazz);
            if (Objects.nonNull(schema)) {
                reference = new WeakReference<>(schema);
                SCHEMA_CACHE.putIfAbsent(clazz, reference);
            } else {
                throw new IllegalStateException("Nonexistent schema");
            }
        }
        return reference;
    }



    // 序列化包装类
    static class SerializationWrapper<T> {
        private T data;

        public SerializationWrapper() {}

        public T getData() {
            return data;
        }

        @SuppressWarnings("rawtypes")
        public SerializationWrapper setData(T data) {
            this.data = data;
            return this;
        }

        @SuppressWarnings("unchecked")
        public static <T> SerializationWrapper<T> builder(T data) {
            return new SerializationWrapper<>().setData(data);
        }
    }
}
