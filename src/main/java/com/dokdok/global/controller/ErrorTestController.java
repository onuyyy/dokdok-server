package com.dokdok.global.controller;

import com.dokdok.global.exception.GlobalErrorCode;
import com.dokdok.global.exception.GlobalException;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"local", "dev"})
@RequestMapping("/api/test")
public class ErrorTestController {

    @GetMapping("/runtime-error")
    public void runtimeError() {
        throw new RuntimeException("webhook test runtime exception");
    }

    @GetMapping("/base-error")
    public void baseError() {
        throw new GlobalException(GlobalErrorCode.INVALID_INPUT_VALUE, "webhook excluded base exception test");
    }
}
