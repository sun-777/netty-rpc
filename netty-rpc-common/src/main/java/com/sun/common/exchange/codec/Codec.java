package com.sun.common.exchange.codec;

/**
 * @description: 自定义RPC协议编码、解码接口
 * @author: Sun Xiaodong
 */
public interface Codec {

    // 编码RPC请求或RPC响应
    byte[] encode(final Object msg);

    // 解码RPC协议为RPC请求或RPC响应
    Object decode(final byte[] data);
}
