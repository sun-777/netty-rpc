package com.sun.common.enumerator;

import com.sun.common.id.serialization.Serializer;
import com.sun.common.id.serialization.impl.ProtostuffSerializer;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @description: 序列化类型枚举类
 * @author: Sun Xiaodong
 */

public enum Serialization implements CodeKeyEnum<Serialization, Byte, Class<? extends Serializer>> {
    PROTOSTUFF((byte) (1 & 0xFF), ProtostuffSerializer.class);

    // 序列化类型
    private final Byte code;
    // 序列化类型对应的序列化Serializer实现类
    private final Class<? extends Serializer> clazz;

    private static final Map<Byte, Serialization> CODE_MAPPER;
    private static final Map<Class<? extends Serializer>, Serialization> KEY_MAPPER;

    static {
        CODE_MAPPER = Arrays.stream(Serialization.values()).collect(Collectors.toMap(Serialization::code, Function.identity()));
        KEY_MAPPER = Arrays.stream(Serialization.values()).collect(Collectors.toMap(Serialization::key, Function.identity()));
    }

    private Serialization(Byte code, Class<? extends Serializer> clazz) {
        if (!Serializer.class.isAssignableFrom(clazz)) {
            throw new IllegalStateException("clazz must be the implementation class of the interface Serializer");
        }
        this.code = code;
        this.clazz = clazz;
    }

    @Override
    public Byte code() {
        return this.code;
    }

    @Override
    public Optional<Serialization> codeOf(Byte c) {
        return Optional.ofNullable(null == c ? null : CODE_MAPPER.get(c));
    }

    @Override
    public Class<? extends Serializer> key() {
        return this.clazz;
    }


    @Override
    public Optional<Serialization> keyOf(Class<? extends Serializer> key) {
        return Optional.ofNullable(null == key ? null : KEY_MAPPER.get(key));
    }

}
