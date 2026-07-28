package com.mina.minaportfoliomanagement.service;

import com.mina.minaportfoliomanagement.client.MarketPriceApiClient;
import com.mina.minaportfoliomanagement.dto.MarketAssetView;
import com.mina.minaportfoliomanagement.dto.MarketQuote;
import com.mina.minaportfoliomanagement.dto.PriceHistoryView;
import com.mina.minaportfoliomanagement.model.AssetCatalog;
import com.mina.minaportfoliomanagement.repository.AssetCatalogRepository;
import com.mina.minaportfoliomanagement.repository.AssetPriceHistoryRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/** 负责市场价格查询和培训 API 的定时同步。 */
@Service
public class MarketPriceService {

    private final AssetCatalogRepository assetCatalogRepository;
    private final AssetPriceHistoryRepository priceHistoryRepository;
    private final MarketPriceApiClient marketPriceApiClient;
    private final PerformanceService performanceService;

    public MarketPriceService(AssetCatalogRepository assetCatalogRepository,
                              AssetPriceHistoryRepository priceHistoryRepository,
                              MarketPriceApiClient marketPriceApiClient,
                              PerformanceService performanceService) {
        this.assetCatalogRepository = assetCatalogRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.marketPriceApiClient = marketPriceApiClient;
        this.performanceService = performanceService;
    }

    public List<MarketAssetView> getMarketAssets() {
        return assetCatalogRepository.findAllWithLatestPrice();
    }

    /** 返回某只股票所有已保存行情，供买入时选择时间点。 */
    public List<PriceHistoryView> getPriceHistory(long assetCatalogId) {
        return priceHistoryRepository.findAllByAssetId(assetCatalogId);
    }

    /** 首次启动立刻拉取价格，保证页面打开时已经有市场数据。 */
    @PostConstruct
    public void loadPricesOnStartup() {
        refreshMarketPrices();
    }

    /** 培训 API 按天更新整段五分钟历史，因此每天早上同步一次即可。 */
    @Scheduled(cron = "0 0 8 * * *")
    public void refreshMarketPricesOnSchedule() {
        refreshMarketPrices();
    }

    /** 拉取全部资产的完整价格历史，并保存为本地数据库的唯一数据来源。 */
    public List<MarketAssetView> refreshMarketPrices() {
        for (AssetCatalog asset : assetCatalogRepository.findAll()) {
            List<MarketQuote> quotes = marketPriceApiClient.getPriceHistory(asset.getTicker());
            priceHistoryRepository.savePrices(asset.getId(), quotes);
        }
        // 全部最新行情写入后，用同一个市场时间保存一次组合总市值。
        performanceService.recordCurrentPortfolioValue();
        return getMarketAssets();
    }
}
