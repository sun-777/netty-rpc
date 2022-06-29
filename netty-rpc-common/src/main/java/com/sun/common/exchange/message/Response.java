package com.sun.common.exchange.message;

/**
 * @description: RPC响应
 * @author: Sun Xiaodong
 */
public final class Response {

    public static final Response VOID = new Response();

    private ResponseHeader header;
    private ResponseBody body;

    public Response() {}

    public Response(final ResponseHeader header, final ResponseBody body) {
        this.header = header;
        this.body = body;
    }

    public ResponseHeader getHeader() {
        return header;
    }

    public Response setHeader(ResponseHeader header) {
        this.header = header;
        return this;
    }

    public ResponseBody getBody() {
        return body;
    }

    public Response setBody(ResponseBody body) {
        this.body = body;
        return this;
    }


    public Response copy() {
        final ResponseHeader header = this.getHeader().copy();
        final ResponseBody body = this.getBody().copy();
        return new Response(header, body);
    }
}
