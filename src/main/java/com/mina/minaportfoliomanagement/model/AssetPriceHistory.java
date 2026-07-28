package com.mina.minaportfoliomanagement.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 对应 MySQL 的 asset_price_history 表，保存可购买资产每五分钟的市场价格。 */
public class AssetPriceHistory {
    private Long id;
    private Long assetCatalogId;
    private BigDecimal marketPrice;
    private LocalDateTime priceTime;

    public AssetPriceHistory() {
    }

    public AssetPriceHistory(Long id, LocalDateTime priceTime, BigDecimal marketPrice, Long assetCatalogId) {
        this.id = id;
        this.priceTime = priceTime;
        this.marketPrice = marketPrice;
        this.assetCatalogId = assetCatalogId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAssetCatalogId() {
        return assetCatalogId;
    }

    public void setAssetCatalogId(Long assetCatalogId) {
        this.assetCatalogId = assetCatalogId;
    }

    public BigDecimal getMarketPrice() {
        return marketPrice;
    }

    public void setMarketPrice(BigDecimal marketPrice) {
        this.marketPrice = marketPrice;
    }

    public LocalDateTime getPriceTime() {
        return priceTime;
    }

    public void setPriceTime(LocalDateTime priceTime) {
        this.priceTime = priceTime;
    }
}
