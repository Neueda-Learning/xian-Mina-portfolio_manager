package com.mina.minaportfoliomanagement.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PortfolioValueHistory {
    private Long id;
    private BigDecimal totalValue;
    private LocalDate recordDate;

    public PortfolioValueHistory() {
    }

    public PortfolioValueHistory(Long id, BigDecimal totalValue, LocalDate recordDate) {
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

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }
}
