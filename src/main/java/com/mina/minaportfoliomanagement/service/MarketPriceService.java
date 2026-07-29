package com.mina.minaportfoliomanagement.service;

import com.mina.minaportfoliomanagement.client.CoinGeckoApiClient;
import com.mina.minaportfoliomanagement.client.MarketPriceApiClient;
import com.mina.minaportfoliomanagement.client.TwelveDataApiClient;
import com.mina.minaportfoliomanagement.dto.MarketAssetView;
import com.mina.minaportfoliomanagement.dto.MarketQuote;
import com.mina.minaportfoliomanagement.dto.PriceHistoryView;
import com.mina.minaportfoliomanagement.model.AssetCatalog;
import com.mina.minaportfoliomanagement.repository.AssetCatalogRepository;
import com.mina.minaportfoliomanagement.repository.AssetPriceHistoryRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/** 负责股票、加密货币市场价格查询和定时同步。 */
@Service
public class MarketPriceService {

    private static final Logger logger = LoggerFactory.getLogger(MarketPriceService.class);
    private static final Map<String, String> COIN_GECKO_IDS = Map.of(
            "BTC", "bitcoin",
            "ETH", "ethereum"
    );

    private final AssetCatalogRepository assetCatalogRepository;
    private final AssetPriceHistoryRepository priceHistoryRepository;
    private final MarketPriceApiClient marketPriceApiClient;
    private final CoinGeckoApiClient coinGeckoApiClient;
    private final TwelveDataApiClient twelveDataApiClient;
    private final PerformanceService performanceService;

    public MarketPriceService(AssetCatalogRepository assetCatalogRepository,
                              AssetPriceHistoryRepository priceHistoryRepository,
                              MarketPriceApiClient marketPriceApiClient,
                              CoinGeckoApiClient coinGeckoApiClient,
                              TwelveDataApiClient twelveDataApiClient,
                              PerformanceService performanceService) {
        this.assetCatalogRepository = assetCatalogRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.marketPriceApiClient = marketPriceApiClient;
        this.coinGeckoApiClient = coinGeckoApiClient;
        this.twelveDataApiClient = twelveDataApiClient;
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

    /** 培训股票 API 按天更新整段五分钟历史，因此每天早上同步一次即可。 */
    @Scheduled(cron = "0 0 8 * * *")
    public void refreshMarketPricesOnSchedule() {
        refreshMarketPrices();
    }

    /** 加密货币接口只返回最新价格，因此每五分钟单独同步一次。 */
    @Scheduled(cron = "0 */5 * * * *")
    public void refreshCryptoPricesOnSchedule() {
        refreshCryptoPrices();
    }

    /** 首次启动或手动刷新时，同时同步股票历史和加密货币最新价格。 */
    public List<MarketAssetView> refreshMarketPrices() {
        assetCatalogRepository.ensureCryptoAssets();
        assetCatalogRepository.ensureFundAssets();
        refreshStockPrices();
        refreshCryptoPrices();
        refreshFundPrices();
        // 全部最新行情写入后，用同一个市场时间保存一次组合总市值。
        performanceService.recordCurrentPortfolioValue();
        return getMarketAssets();
    }

    /** 股票 API 返回完整五分钟历史，因此每次覆盖式补齐到本地价格历史表。 */
    private void refreshStockPrices() {
        for (AssetCatalog asset : assetCatalogRepository.findAllStocks()) {
            List<MarketQuote> quotes = marketPriceApiClient.getPriceHistory(asset.getTicker());
            priceHistoryRepository.savePrices(asset.getId(), quotes);
        }
    }

    /** CoinGecko 一次获取 BTC、ETH；缺少 Key 或网络失败时不影响原有股票功能启动。 */
    private void refreshCryptoPrices() {
        if (!coinGeckoApiClient.isConfigured()) {
            logger.warn("COINGECKO_API_KEY is not configured. Crypto prices are skipped.");
            return;
        }

        try {
            assetCatalogRepository.ensureCryptoAssets();
            Map<String, MarketQuote> quotes = coinGeckoApiClient.getLatestPrices();
            for (AssetCatalog asset : assetCatalogRepository.findAllCrypto()) {
                String coinGeckoId = COIN_GECKO_IDS.get(asset.getTicker());
                MarketQuote quote = quotes.get(coinGeckoId);
                if (quote != null) {
                    priceHistoryRepository.savePrices(asset.getId(), List.of(quote));
                }
            }
        } catch (RuntimeException exception) {
            logger.warn("CoinGecko price synchronization failed. Existing prices are kept.", exception);
        }
    }

    /**
     * 基金同步的是每日公布的净值，因此只在项目启动、手动刷新和每日定时刷新时执行。
     * 与每五分钟同步一次的加密货币任务分开，避免浪费 Twelve Data 的请求额度。
     */
    private void refreshFundPrices() {
        if (!twelveDataApiClient.isConfigured()) {
            logger.warn("TWELVE_DATA_API_KEY is not configured. Fund prices are skipped.");
            return;
        }

        try {
            for (AssetCatalog asset : assetCatalogRepository.findAllFunds()) {
                // Twelve Data 返回最近 30 个交易日；重复日期会由数据库唯一键自动更新。
                List<MarketQuote> quotes = twelveDataApiClient.getFundPriceHistory(asset.getTicker());
                priceHistoryRepository.savePrices(asset.getId(), quotes);
                logger.info("Synchronized {} daily fund prices for {}", quotes.size(), asset.getTicker());
            }
        } catch (RuntimeException exception) {
            logger.warn("Fund price synchronization failed. Existing prices are kept.", exception);
        }
    }
}
