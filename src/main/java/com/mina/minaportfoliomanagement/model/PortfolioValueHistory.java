package com.mina.minaportfoliomanagement.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PortfolioValueHistory {
    private Long id;
    private BigDecimal totalValue;
    private LocalDateTime recordDate;

    public PortfolioValueHistory() {
    }

    public PortfolioValueHistory(Long id, BigDecimal totalValue, LocalDateTime recordDate) {
        this.id = id;
        this.totalValue = totalValue;
        this.recordDate = recordDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public LocalDateTime getRecordDate() {
        return recordDate;
    }
    public void setRecordDate(LocalDateTime recordDate) {
        this.recordDate = recordDate;
    }
}
