package com.mina.minaportfoliomanagement.model;

public class AssetCatalog {
    private Long id;
    private String ticker;
    private String assetName;
    private String assetType;

    public AssetCatalog() {
    }

    public AssetCatalog(Long id, String ticker, String assetName, String assetType) {
        this.id = id;
        this.ticker = ticker;
        this.assetName = assetName;
        this.assetType = assetType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }
}
