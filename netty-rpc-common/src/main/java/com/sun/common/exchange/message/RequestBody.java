package com.sun.common.exchange.message;

/**
 * @description: RPC请求消息体
 * @author: Sun Xiaodong
 */
public final class RequestBody {
    /**
     * 调用接口全限定名称
     */
    private String interfaceName;
    /**
     * 调用的方法名
     */
    private String methodName;
    /**
     * 方法返回类型
     */
    private Class<?> returnType;

    /**
     * 方法参数类型列表
     * this.parameterTypes = parameterTypes == null ? new Class<?>[0] : parameterTypes;
     */
    private Class<?>[] parameterTypes;
    /**
     * 方法参数
     */
    private Object[] parameters;

    public RequestBody() {}

    public String getInterfaceName() {
        return interfaceName;
    }

    public RequestBody setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
        return this;
    }

    public String getMethodName() {
        return methodName;
    }

    public RequestBody setMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public RequestBody setReturnType(Class<?> returnType) {
        this.returnType = returnType;
        return this;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public RequestBody setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
        return this;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public RequestBody setParameters(Object[] parameters) {
        this.parameters = parameters;
        return this;
    }
}
