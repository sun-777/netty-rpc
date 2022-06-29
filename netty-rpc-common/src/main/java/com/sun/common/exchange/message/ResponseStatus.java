package com.sun.common.exchange.message;

/**
 * @description: 响应状态
 * @author: Sun Xiaodong
 */
public interface ResponseStatus {
    byte OK = 1;
    /**
     * client side timeout
     */
    byte CLIENT_TIMEOUT = 2;
    /**
     * server side timeout
     */
    byte SERVER_TIMEOUT = 3;
    /**
     * channel inactive, should directly return the unfinished requests.
     */
    byte CHANNEL_INACTIVE = 4;
    /**
     * request format error
     */
    byte BAD_REQUEST = 5;
    /**
     * response format error
     */
    byte BAD_RESPONSE = 6;
    /**
     * service error
     */
    byte SERVICE_ERROR = 7;
    /**
     * service not found
     */
    byte SERVICE_NOT_FOUND = 8;
    /**
     * internal server error
     */
    byte SERVER_ERROR = 9;
    /**
     * internal client error
     */
    byte CLIENT_ERROR = 10;
    /**
     * server side threadpool resource exhausted and quick return
     */
    byte SERVER_THREADPOOL_RESOURCE_EXHAUSTED_ERROR = 11;
}
