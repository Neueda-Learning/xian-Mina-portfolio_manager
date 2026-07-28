package com.mina.minaportfoliomanagement.service;

import com.mina.minaportfoliomanagement.dto.HoldingView;
import com.mina.minaportfoliomanagement.repository.AssetCatalogRepository;
import com.mina.minaportfoliomanagement.repository.AssetPriceHistoryRepository;
import com.mina.minaportfoliomanagement.repository.PortfolioItemRepository;
import com.mina.minaportfoliomanagement.repository.TradeHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Service
public class HoldingService {
    private final PortfolioItemRepository portfolioItemRepository;
    private final AssetCatalogRepository assetCatalogRepository;
    private final AssetPriceHistoryRepository priceHistoryRepository;

    private final TradeHistoryRepository tradeHistoryRepository;


    public HoldingService(PortfolioItemRepository portfolioItemRepository, AssetCatalogRepository assetCatalogRepository, AssetPriceHistoryRepository priceHistoryRepository, TradeHistoryRepository tradeHistoryRepository) {
        this.portfolioItemRepository = portfolioItemRepository;
        this.assetCatalogRepository = assetCatalogRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.tradeHistoryRepository = tradeHistoryRepository;
    }
}
