package com.mina.minaportfoliomanagement.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PortfolioItem {
    private Long id;
    private Long assetCatalogId;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private LocalDate purchaseDate;
    private LocalDateTime createdAt;

    public PortfolioItem() {
    }

    public PortfolioItem(Long id, Long assetCatalogId, BigDecimal quantity, BigDecimal purchasePrice, LocalDate purchaseDate, LocalDateTime createdAt) {
        this.id = id;
        this.assetCatalogId = assetCatalogId;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.purchaseDate = purchaseDate;
        this.createdAt = createdAt;
    }

    public Long getAssetCatalogId() {
        return assetCatalogId;
    }

    public void setAssetCatalogId(Long assetCatalogId) {
        this.assetCatalogId = assetCatalogId;
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

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
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
