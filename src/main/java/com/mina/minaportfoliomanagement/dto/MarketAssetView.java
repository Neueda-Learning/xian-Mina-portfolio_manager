package com.mina.minaportfoliomanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 给市场页面返回的数据。
 * 它包含资产目录信息和当天最新市场价，
 * 因此不是单独对应某一张数据库表的 Model。
 */
public class MarketAssetView {

    private final Long id;
    private final String ticker;
    private final String assetName;
    private final String assetType;
    private final BigDecimal marketPrice;
    private final LocalDate priceDate;

    public MarketAssetView(Long id, String ticker, String assetName, String assetType,
                           BigDecimal marketPrice, LocalDate priceDate) {
        this.id = id;
        this.ticker = ticker;
        this.assetName = assetName;
        this.assetType = assetType;
        this.marketPrice = marketPrice;
        this.priceDate = priceDate;
    }

    public Long getId() {
        return id;
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

    public BigDecimal getMarketPrice() {
        return marketPrice;
    }

    public LocalDate getPriceDate() {
        return priceDate;
    }
}
