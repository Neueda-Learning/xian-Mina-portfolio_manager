package com.mina.minaportfoliomanagement.controller;

import com.mina.minaportfoliomanagement.dto.MarketAssetView;
import com.mina.minaportfoliomanagement.dto.PriceHistoryView;
import com.mina.minaportfoliomanagement.service.MarketPriceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 对前端提供可交易资产、历史行情和手动同步接口。 */
@RestController
@RequestMapping("/api/portfolio-items/market")
public class MarketPriceController {

    private final MarketPriceService marketPriceService;

    public MarketPriceController(MarketPriceService marketPriceService) {
        this.marketPriceService = marketPriceService;
    }

    @GetMapping("/assets")
    public List<MarketAssetView> getMarketAssets() {
        return marketPriceService.getMarketAssets();
    }

    @GetMapping("/assets/{assetCatalogId}/prices")
    public List<PriceHistoryView> getPriceHistory(@PathVariable long assetCatalogId) {
        return marketPriceService.getPriceHistory(assetCatalogId);
    }

    @PostMapping("/refresh")
    public List<MarketAssetView> refreshMarketPrices() {
        return marketPriceService.refreshMarketPrices();
    }
}
