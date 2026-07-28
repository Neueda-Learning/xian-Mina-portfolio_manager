package com.mina.minaportfoliomanagement.controller;

import com.mina.minaportfoliomanagement.dto.PortfolioSummary;
import com.mina.minaportfoliomanagement.service.SummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SummaryController {
    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/summary")
    public PortfolioSummary getSummary() {
        return summaryService.getSummary();
    }
}
