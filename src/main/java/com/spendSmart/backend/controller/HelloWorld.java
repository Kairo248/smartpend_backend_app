package com.spendSmart.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorld {
    @GetMapping("/api/hello")
    public String hello() {
        return "Backend is running 🚀";
    }
}
