package com.sun.client.controller;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Objects;

/**
 * @description: Controller接口类，提供默认的获取请求对象、响应对象的接口
 * @author: Sun Xiaodong
 */
public interface BaseController {

    default HttpServletRequest request() {
        return ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
    }

    default HttpServletResponse response() {
        return ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getResponse();
    }

    /**
     * 获取Session
     * @param allowCreate  如果没有Session，是否创建一个HttpSession
     * @return
     */
    default HttpSession session(boolean allowCreate) {
        return request().getSession(allowCreate);
    }
}
