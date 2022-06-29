package com.sun.common.exchange.message;

/**
 * @description: RPC请求
 * @author: Sun Xiaodong
 */
public final class Request {
    private RequestHeader header;
    private RequestBody body;

    public Request() {}

    public Request(final RequestHeader header, final RequestBody body) {
        this.header = header;
        this.body = body;
    }

    public RequestHeader getHeader() {
        return header;
    }

    public Request setHeader(RequestHeader header) {
        this.header = header;
        return this;
    }

    public RequestBody getBody() {
        return body;
    }

    public Request setBody(RequestBody body) {
        this.body = body;
        return this;
    }

}
