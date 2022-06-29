package com.sun.common.service;

import com.sun.common.annotation.RpcServiceInterface;

/**
 * @description:
 * @author: Sun Xiaodong
 */

@RpcServiceInterface
public interface HeartbeatService {
    String ping();
}
