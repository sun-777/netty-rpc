package com.sun.common.id.serialization;

/**
 * @description: 序列化接口
 * @author: Sun Xiaodong
 */
public interface Serializer {
    /**
     * 反序列化接口， 将字节数组反序列化为T类的对象
     * @param <T> 声明接口为泛型接口
     * @param clazz 泛型T代表的类
     * @param data 对象的字节数组
     * @return 返回值类型为T类
     */
    <T> T deserialize(Class<T> clazz, byte[] data) throws RuntimeException;

    /**
     * 序列化接口， 将T类的对象序列化为字节数组
     * @param <T> 明接口为泛型接口
     * @param object 泛型T代表的类的对象
     * @return 返回值类型为字节数组
     */
    <T> byte[] serialize(T object) throws RuntimeException;
}
