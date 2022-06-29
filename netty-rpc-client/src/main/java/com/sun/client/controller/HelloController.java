package com.sun.client.controller;

import com.sun.common.service.HelloService;
import com.sun.common.util.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description:
 * @author: Sun Xiaodong
 */

@RestController
public class HelloController implements BaseController {
    @Resource
    private HelloService helloService;

    @GetMapping("/sayHello")
    public Result<String> hello(@RequestParam("name") String name) {
        try {
            String hello = helloService.sayHello(name);
            return Result.success(hello);
        } catch (Exception e) {
            return Result.error(null, e);
        }
    }

    @GetMapping("/random")
    public Result<List<Integer>> random() {
        try {
            List<Integer> list = helloService.random();
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(null, e);
        }
    }
}