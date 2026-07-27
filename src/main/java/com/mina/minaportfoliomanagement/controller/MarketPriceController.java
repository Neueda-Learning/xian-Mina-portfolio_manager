package com.mina.minaportfoliomanagement.controller;

import com.mina.minaportfoliomanagement.dto.MarketAssetView;
import com.mina.minaportfoliomanagement.service.MarketPriceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio-items/market")
public class MarketPriceController {

    private final MarketPriceService marketPriceService;

    public MarketPriceController(MarketPriceService service) {
        this.marketPriceService = service;
    }

    @GetMapping("/assets")
    public List<MarketAssetView> getMarketAssets() {
        return marketPriceService.getMarketAssets();
    }
    /**
     * 生成下一模拟交易日的市场价格。
     */
    @PostMapping("/next-day")
    public List<MarketAssetView> advanceToNextDay() {
        return marketPriceService.advanceToNextDay();
    }
}
