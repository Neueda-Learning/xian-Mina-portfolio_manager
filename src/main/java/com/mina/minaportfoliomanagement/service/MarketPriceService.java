package com.mina.minaportfoliomanagement.service;

import com.mina.minaportfoliomanagement.dto.MarketAssetView;
import com.mina.minaportfoliomanagement.repository.AssetCatalogRepository;
import com.mina.minaportfoliomanagement.repository.AssetPriceHistoryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MarketPriceService {
    private final AssetCatalogRepository assetCatalogRepository;
    private final AssetPriceHistoryRepository priceHistoryRepository;

    public MarketPriceService(AssetCatalogRepository assetCatalogRepository, AssetPriceHistoryRepository priceHistoryRepository) {
        this.assetCatalogRepository = assetCatalogRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }
    public List<MarketAssetView> getMarketAssets() {
        return assetCatalogRepository.findAllWithLatestPrice();
    }
    /**
     * 为全部资产生成下一模拟交易日的价格。
     */
    public List<MarketAssetView> advanceToNextDay() {
        List<MarketAssetView> currentAssets =
                assetCatalogRepository.findAllWithLatestPrice();

        LocalDate nextDate =
                priceHistoryRepository.getLatestMarketDate().plusDays(1);

        List<MarketAssetView> nextAssets = new ArrayList<>();

        for (MarketAssetView asset : currentAssets) {
            BigDecimal nextPrice = randomNextPrice(asset.getMarketPrice());

            priceHistoryRepository.savePrice(
                    asset.getId(),
                    nextPrice,
                    nextDate
            );

            nextAssets.add(new MarketAssetView(
                    asset.getId(),
                    asset.getTicker(),
                    asset.getAssetName(),
                    asset.getAssetType(),
                    nextPrice,
                    nextDate
            ));
        }

        return nextAssets;
    }

    /**
     * 价格随机浮动 -5% 到 +5%，最低不能小于 0.01。
     */
    private BigDecimal randomNextPrice(BigDecimal currentPrice) {
        double change =
                ThreadLocalRandom.current().nextDouble(-0.05, 0.0501);

        BigDecimal nextPrice = currentPrice
                .multiply(BigDecimal.valueOf(1 + change))
                .setScale(2, RoundingMode.HALF_UP);

        return nextPrice.max(new BigDecimal("0.01"));
    }

}
