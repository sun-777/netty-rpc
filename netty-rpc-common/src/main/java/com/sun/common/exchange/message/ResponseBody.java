package com.sun.common.exchange.message;

/**
 * @description: RPC响应消息体
 * @author: Sun Xiaodong
 */
public final class ResponseBody {
    private boolean success;
    private Object result;
    private String errorMsg;

    private ResponseBody() {}

    /**
     * 响应OK
     * @param result 响应结果
     */
    public ResponseBody(Object result) {
        this.success = true;
        this.result = result;
    }

    /**
     * 响应错误
     * @param result  响应结果
     * @param errorMsg  错误信息
     */
    public ResponseBody(Object result, String errorMsg) {
        this.success = false;
        this.result = result;
        this.errorMsg = errorMsg;
    }


    public ResponseBody copy() {
        return new ResponseBody()
                    .setSuccess(this.getSuccess())
                    .setResult(this.getResult())
                    .setErrorMsg(this.getErrorMsg());

    }

    public ResponseBody setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public boolean getSuccess() {
        return success;
    }


    private ResponseBody setResult(Object result) {
        this.result = result;
        return this;
    }

    public Object getResult() {
        return result;
    }

    private ResponseBody setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
        return this;
    }

    public String getErrorMsg() {
        return this.errorMsg;
    }

}
