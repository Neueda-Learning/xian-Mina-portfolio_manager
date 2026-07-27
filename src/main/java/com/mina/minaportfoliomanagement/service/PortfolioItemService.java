package com.mina.minaportfoliomanagement.service;

import com.mina.minaportfoliomanagement.dto.MarketAssetView;
import com.mina.minaportfoliomanagement.repository.AssetCatalogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PortfolioItemService {
    private final AssetCatalogRepository assetCatalogRepository;

    public PortfolioItemService(AssetCatalogRepository assetCatalogRepository) {
        this.assetCatalogRepository = assetCatalogRepository;
    }
    public List<MarketAssetView> getMarketAssets() {
        return assetCatalogRepository.findAllWithLatestPrice();
    }


}
