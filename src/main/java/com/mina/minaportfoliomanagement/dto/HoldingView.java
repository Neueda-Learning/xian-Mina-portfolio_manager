package com.mina.minaportfoliomanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class HoldingView {
    private Long id;
    private Long assetCatalogId;
    private String ticker;
    private String assetName;
    private String assetType;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private BigDecimal currentPrice;
    private LocalDateTime createdAt;

    public HoldingView(Long id, Long assetCatalogId, String ticker, String assetName, String assetType, BigDecimal quantity, BigDecimal purchasePrice, BigDecimal currentPrice, LocalDateTime createdAt) {
        this.id = id;
        this.assetCatalogId = assetCatalogId;
        this.ticker = ticker;
        this.assetName = assetName;
        this.assetType = assetType;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.currentPrice = currentPrice;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getAssetCatalogId() {
        return assetCatalogId;
    }

    public String getTicker() {
        return ticker;
    }

    public String getAssetName() {
        return assetName;
    }

    public String getAssetType() {
        return assetType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
