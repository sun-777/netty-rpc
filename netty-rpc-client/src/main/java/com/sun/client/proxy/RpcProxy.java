package com.sun.client.proxy;

import com.sun.client.context.RpcClient;
import com.sun.common.enumerator.Event;
import com.sun.common.exchange.message.Request;
import com.sun.common.exchange.message.RequestBody;
import com.sun.common.exchange.message.RequestHeader;
import com.sun.common.exchange.message.Response;
import com.sun.common.exchange.message.ResponseBody;
import com.sun.common.exchange.message.ResponseStatus;
import com.sun.common.service.HeartbeatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @description: 动态代理
 * @author: Sun Xiaodong
 */

@Component
public class RpcProxy {
    @Resource
    private RpcClient client;

    public <T> Object getProxy(final Class<T> targetClass) {
        if (Objects.isNull(targetClass)) {
            return null;
        }

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(targetClass);
        enhancer.setUseCache(true);
        enhancer.setCallback(new RpcProxy.SimpleMethodInterceptor(targetClass));
        return enhancer.create();
    }


    private class SimpleMethodInterceptor implements MethodInterceptor, Serializable {
        private static final long serialVersionUID = -2693329602568707674L;
        private final transient InvocationHandler invocationHandler;

        public <T> SimpleMethodInterceptor(Class<T> targetClass) {
            this.invocationHandler = newInvocationHandler(targetClass);
        }

        @Override
        public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
            return this.invocationHandler.invoke(o, method, objects);
        }

        private <T> InvocationHandler newInvocationHandler(Class<T> targetClass) {
            return (proxy, method, args) -> {
                if (Object.class == method.getDeclaringClass()) {
                    String methodName = method.getName();
                    switch (methodName) {
                        case "equals":
                            return proxy == args[0];
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "toString":
                            return proxy.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(proxy)) +
                                    ", with InvocationHandler " + this;
                        default:
                            throw new IllegalStateException(method.toString());
                    }
                }

                final Request request = assembleRequest(targetClass, method, args);
                CompletableFuture<Response> future = RpcProxy.this.client.sendRequest(request);
                if (Objects.nonNull(future)) {
                    final String requestId = request.getHeader().getId().toString();
                    final int timeoutMillis = RpcProxy.this.client.properties.getTimeout();
                    try {
                        Response response;
                        if (timeoutMillis <= 0) {
                            response = future.get();
                        } else {
                            response = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
                        }
                        // 成功响应，则从responseFutures中移除此future对象
                        RpcProxy.this.client.removeCompleteFutureObject(requestId);

                        // 处理正常结果和异常结果
                        // （异常结果是服务端返回的结果，不可被捕获，必需交由调用者处理）
                        ResponseBody body  = Objects.requireNonNull(response.getBody());
                        if (ResponseStatus.OK == response.getHeader().getStatus()) {
                            return body.getResult();
                        }
                        throw new RuntimeException(body.getErrorMsg(), (Throwable) body.getResult());
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        // 仅需捕获future.get方法抛出的异常；
                        RpcProxy.this.client.completeCompleteFutureObject(requestId);
                        throw e;
                    }
                } else {
                    return null;
                }
            };
        }

        // 组装request
        private <T> Request assembleRequest(final Class<T> targetClass, final Method method, final Object[] args) {
            final RequestHeader header = (RequestHeader) RequestHeader
                    .getDefault(RpcProxy.this.client.properties.getTimeout())
                    .setSerialization(RpcProxy.this.client.properties.getSerializer());
            // 根据事件服务接口类型，设置事件
            if (HeartbeatService.class.equals(targetClass)) { // 设置事件
                header.setEvent(Event.HEARTBEAT);
            }

            final RequestBody body = new RequestBody()
                    .setInterfaceName(method.getDeclaringClass().getName())
                    .setMethodName(method.getName())
                    .setReturnType(method.getReturnType())
                    .setParameterTypes(method.getParameterTypes())
                    .setParameters(args);
            return new Request(header, body);
        }

    }
}
