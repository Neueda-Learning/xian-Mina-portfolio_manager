package com.mina.minaportfoliomanagement.controller;

import com.mina.minaportfoliomanagement.dto.PortfolioSummary;
import com.mina.minaportfoliomanagement.service.SummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio-items")
public class SummaryController {
    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/summary")
    public PortfolioSummary getSummary(@RequestParam(required = false) Long portfolioId) {
        return summaryService.getSummary(portfolioId);
    }
}
