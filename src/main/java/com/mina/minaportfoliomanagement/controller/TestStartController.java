package com.mina.minaportfoliomanagement.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestStartController {
    @GetMapping("/api/health")
    public String health() {
        return "Portfolio Management is running.";
    }
}
