package com.sun.common.exchange.message;

import com.sun.common.id.ObjectId;

/**
 * @description: RPC请求消息头
 * @author: Sun Xiaodong
 */
public final class RequestHeader extends Header {

    /**
     * 客户端请求超时时间，单位：ms
     * 请求时，写入的超时时间是相对时间。
     */
    private final int timeoutMillis;

    /**
     * 消息请求是否需要响应
     */
    private boolean responseRequired;

    private RequestHeader() {
        super();
        this.responseRequired = true;
        this.timeoutMillis = 0;
    }

    private RequestHeader(int timeoutMillis) {
        super();
        this.responseRequired = true;
        this.timeoutMillis = timeoutMillis;
    }

    public int getTimeoutMillis() {
        return this.timeoutMillis;
    }

    // timeoutMillis
    public boolean getResponseRequired() {
        return this.responseRequired;
    }

    public RequestHeader setResponseRequired(boolean responseRequired) {
        this.responseRequired = responseRequired;
        return this;
    }


    /**
     * 生成默认的请求头
     * @return
     */
    public static RequestHeader getDefault() {
        return (RequestHeader) new RequestHeader().setId(ObjectId.get());
    }

    /**
     * 生成指定超时时间的请求头
     * @param timeoutMillis  超时时间
     * @return
     */
    public static RequestHeader getDefault(int timeoutMillis) {
        return (RequestHeader) new RequestHeader(timeoutMillis)
                .setId(ObjectId.get());
    }
}
