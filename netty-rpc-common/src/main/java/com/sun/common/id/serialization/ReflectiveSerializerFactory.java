package com.sun.common.id.serialization;

import java.lang.reflect.Constructor;

import io.netty.channel.ChannelException;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;

/**
 * @description:
 * @author: Sun Xiaodong
 */
public class ReflectiveSerializerFactory<T extends Serializer> implements SerializerFactory<T> {
    private final Constructor<? extends T> constructor;

    public ReflectiveSerializerFactory(Class<? extends T> clazz) {
        ObjectUtil.checkNotNull(clazz, "clazz");
        try {
            this.constructor = clazz.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Class " + StringUtil.simpleClassName(clazz) + " does not have a public non-arg constructor", e);
        }
    }


    @Override
    public T newSerializer() {
        try {
            return (T) constructor.newInstance();
        } catch (Throwable t) {
            throw new ChannelException("Unable to create Channel from class " + constructor.getDeclaringClass(), t);
        }
    }
}
