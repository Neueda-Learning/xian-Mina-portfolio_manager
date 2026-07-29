package com.mina.minaportfoliomanagement.controller;

import com.mina.minaportfoliomanagement.dto.CreatePortfolioRequest;
import com.mina.minaportfoliomanagement.model.Portfolio;
import com.mina.minaportfoliomanagement.service.PortfolioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {
    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public List<Portfolio> getPortfolios() {
        return portfolioService.getAllPortfolios();
    }

    @PostMapping
    public ResponseEntity<Portfolio> createPortfolio(@RequestBody CreatePortfolioRequest request) {
        Portfolio portfolio = portfolioService.createPortfolio(request.getPortfolioName());
        return ResponseEntity.status(HttpStatus.CREATED).body(portfolio);
    }
}
