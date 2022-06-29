package com.sun.common.exchange.message;

import com.sun.common.enumerator.Event;
import com.sun.common.enumerator.Serialization;
import com.sun.common.id.serialization.Serializer;
import com.sun.common.id.serialization.ReflectiveSerializerFactory;
import com.sun.common.id.serialization.SerializerFactory;
import com.sun.common.id.Id;

/**
 * @description: RPC消息头
 * @author: Sun Xiaodong
 */
public class Header {
    /**
     * 请求ID，用于标识本次请求以匹配RPC服务器的响应
     */
    private Id id;

    /**
     * 序列化类型，默认: Serialization.PROTOBUF
     */
    private Serialization serialization;

    /**
     * 事件，默认0（即：0表示非事件）
     */
    private Event event;


    public Header() {
        this.serialization = Serialization.PROTOSTUFF;
        this.event = Event.NONE;
    }

    public Id getId() {
        return this.id;
    }

    public Header setId(Id id) {
        this.id = id;
        return this;
    }

    public Serialization getSerialization() {
        return this.serialization;
    }

    public Header setSerialization(Serialization serialization) {
        this.serialization = serialization;
        return this;
    }

    public Event getEvent() {
        return this.event;
    }

    public Header setEvent(Event event) {
        this.event = event;
        return this;
    }

    /**
     * 序列化工厂类
     * @return 返回SerializerFactory对象
     */
    public SerializerFactory<? extends Serializer> serializerFactory() {
        return new ReflectiveSerializerFactory<>(serialization.key());
    }

}
