package com.sun.common.enumerator;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @description: 定义事件枚举类
 *               因协议（见ExchangeCodec类）设计，目前使用3 bits表示事件，所以最多只能定义从1~7的7个事件（非0为有效事件）。
 * @author: Sun Xiaodong
 */
public enum Event implements CodeEnum<Event, Byte> {
    // NONE: 非事件
    NONE((byte) 0),
    // 心跳
    HEARTBEAT((byte) 1),
    // 文件上传
    FILE_UPLOAD((byte) 2);

    private final Byte code;
    private static final Map<Byte, Event> CODE_MAPPER;

    static {
        CODE_MAPPER = Arrays.stream(Event.values()).collect(Collectors.toMap(Event::code, Function.identity()));
    }

    private Event(Byte code) {
        this.code = code;
    }


    @Override
    public Byte code() {
        return this.code;
    }


    @Override
    public Optional<Event> codeOf(Byte c) {
        return Optional.ofNullable(null == c ? null : CODE_MAPPER.get(c));
    }
}
