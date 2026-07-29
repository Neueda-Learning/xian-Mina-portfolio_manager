package com.mina.minaportfoliomanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BuyRequest {
    private Long assetCatalogId;
    private BigDecimal quantity;
    private LocalDateTime priceTime;

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

    public LocalDateTime getPriceTime() {
        return priceTime;
    }

    public void setPriceTime(LocalDateTime priceTime) {
        this.priceTime = priceTime;
    }
}
