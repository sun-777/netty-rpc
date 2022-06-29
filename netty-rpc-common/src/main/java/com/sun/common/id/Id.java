package com.sun.common.id;

import java.io.Serializable;

/**
 * @description: Id接口类
 * @author: Sun Xiaodong
 */
public interface Id extends Serializable {

    String toString();

    // 将Id转换为byte数组，且长度不超过ObjectId::OBJECT_ID_LENGTH
    byte[] toByteArray();

    // 将bytes数组转为Id对象
    Id parse(final byte[] array);

    // IdType类型见ExchangeCodec类中的定义
    // 当新增一个Id接口的实现类时，需要手动定义与之对应的新的IdType，IdType取值范围: 0 ~ 7
    short getIdType();
}
