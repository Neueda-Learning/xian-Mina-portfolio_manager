package com.mina.minaportfoliomanagement.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PortfolioItem {
    private Long id;
    private Long portfolioId;
    private Long assetCatalogId;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private LocalDateTime purchaseTime;
    private LocalDateTime createdAt;

    public PortfolioItem() {
    }

    public PortfolioItem(Long id, Long portfolioId, Long assetCatalogId, BigDecimal quantity, BigDecimal purchasePrice, LocalDateTime purchaseTime, LocalDateTime createdAt) {
        this.id = id;
        this.portfolioId = portfolioId;
        this.assetCatalogId = assetCatalogId;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.purchaseTime = purchaseTime;
        this.createdAt = createdAt;
    }

    public Long getAssetCatalogId() {
        return assetCatalogId;
    }

    public void setAssetCatalogId(Long assetCatalogId) {
        this.assetCatalogId = assetCatalogId;
    }

    public Long getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(Long portfolioId) {
        this.portfolioId = portfolioId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public LocalDateTime getPurchaseTime() {
        return purchaseTime;
    }

    public void setPurchaseTime(LocalDateTime purchaseTime) {
        this.purchaseTime = purchaseTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
