package com.mina.minaportfoliomanagement.controller;

import com.mina.minaportfoliomanagement.model.PortfolioValueHistory;
import com.mina.minaportfoliomanagement.service.PerformanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio-items")
public class PerformanceController {
    private final PerformanceService performanceService;

    public PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @GetMapping("/performance")
    public List<PortfolioValueHistory> getPerformance(@RequestParam(required = false) Long portfolioId) {
        return performanceService.getPerformance(portfolioId);
    }
}
