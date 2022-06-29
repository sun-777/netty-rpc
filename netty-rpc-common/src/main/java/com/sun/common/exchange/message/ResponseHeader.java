package com.sun.common.exchange.message;

/**
 * @description: RPC响应消息头
 * @author: Sun Xiaodong
 */
public final class ResponseHeader extends Header {

    // 响应状态（见ResponseStatus类定义的状态）
    private byte status;

    private ResponseHeader() {
        super();
    }

    public byte getStatus() {
        return status;
    }

    public ResponseHeader setStatus(byte status) {
        this.status = status;
        return this;
    }

    public ResponseHeader copy() {
        return (ResponseHeader) new ResponseHeader()
                .setStatus(this.getStatus())
                .setId(this.getId())
                .setEvent(this.getEvent())
                .setSerialization(this.getSerialization());
    }

    public static ResponseHeader getDefault() {
        return new ResponseHeader();
    }
}
