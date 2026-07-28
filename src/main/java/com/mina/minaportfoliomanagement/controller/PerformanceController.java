package com.mina.minaportfoliomanagement.controller;

import com.mina.minaportfoliomanagement.model.PortfolioValueHistory;
import com.mina.minaportfoliomanagement.service.PerformanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PerformanceController {
    private final PerformanceService performanceService;

    public PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @GetMapping("/performance")
    public List<PortfolioValueHistory> getPerformance() {
        return performanceService.getPerformance();
    }
}
