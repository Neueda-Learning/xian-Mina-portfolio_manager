package com.mina.minaportfoliomanagement.model;

import java.time.LocalDateTime;

public class Portfolio {
    private Long id;
    private String portfolioName;
    private LocalDateTime createdAt;

    public Portfolio() {
    }

    public Portfolio(Long id, String portfolioName, LocalDateTime createdAt) {
        this.id = id;
        this.portfolioName = portfolioName;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getPortfolioName() {
        return portfolioName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
