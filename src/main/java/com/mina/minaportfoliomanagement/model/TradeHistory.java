package com.mina.minaportfoliomanagement.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TradeHistory {

    private Long assetCatalogId;
    private Long portfolioId;
    private String tradeType;
    private BigDecimal quantity;
    private BigDecimal tradePrice;
    private LocalDateTime tradeTime;

    public TradeHistory() {
    }

    public TradeHistory(Long assetCatalogId, Long portfolioId, String tradeType, BigDecimal quantity, BigDecimal tradePrice, LocalDateTime tradeTime) {
        this.assetCatalogId = assetCatalogId;
        this.portfolioId = portfolioId;
        this.tradeType = tradeType;
        this.quantity = quantity;
        this.tradePrice = tradePrice;
        this.tradeTime = tradeTime;
    }

    public Long getAssetCatalogId() {
        return assetCatalogId;
    }

    public Long getPortfolioId() {
        return portfolioId;
    }

    public String getTradeType() {
        return tradeType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getTradePrice() {
        return tradePrice;
    }

    public LocalDateTime getTradeTime() {
        return tradeTime;
    }

}
