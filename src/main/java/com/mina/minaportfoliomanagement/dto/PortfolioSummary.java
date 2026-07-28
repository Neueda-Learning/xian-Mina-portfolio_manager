package com.mina.minaportfoliomanagement.dto;

import java.math.BigDecimal;
import java.util.List;

public class PortfolioSummary {
    private int itemCount;
    private BigDecimal totalCost;
    private BigDecimal totalMarketValue;
    private BigDecimal totalReturn;
    private BigDecimal returnPercent;
    private List<HoldingView> items;


    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getTotalMarketValue() {
        return totalMarketValue;
    }

    public void setTotalMarketValue(BigDecimal totalMarketValue) {
        this.totalMarketValue = totalMarketValue;
    }

    public BigDecimal getTotalReturn() {
        return totalReturn;
    }

    public void setTotalReturn(BigDecimal totalReturn) {
        this.totalReturn = totalReturn;
    }

    public BigDecimal getReturnPercent() {
        return returnPercent;
    }

    public void setReturnPercent(BigDecimal returnPercent) {
        this.returnPercent = returnPercent;
    }

    public List<HoldingView> getItems() {
        return items;
    }

    public void setItems(List<HoldingView> items) {
        this.items = items;
    }
}
