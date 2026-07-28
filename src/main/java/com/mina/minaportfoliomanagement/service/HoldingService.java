package com.mina.minaportfoliomanagement.service;

import com.mina.minaportfoliomanagement.dto.BuyRequest;
import com.mina.minaportfoliomanagement.dto.HoldingView;
import com.mina.minaportfoliomanagement.dto.MarketAssetView;
import com.mina.minaportfoliomanagement.dto.SellRequest;
import com.mina.minaportfoliomanagement.model.PortfolioItem;
import com.mina.minaportfoliomanagement.model.TradeHistory;
import com.mina.minaportfoliomanagement.repository.AssetCatalogRepository;
import com.mina.minaportfoliomanagement.repository.AssetPriceHistoryRepository;
import com.mina.minaportfoliomanagement.repository.PortfolioItemRepository;
import com.mina.minaportfoliomanagement.repository.TradeHistoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class HoldingService {
    private final PortfolioItemRepository portfolioItemRepository;
    private final AssetCatalogRepository assetCatalogRepository;
    private final AssetPriceHistoryRepository priceHistoryRepository;
    private final PerformanceService performanceService;
    private final TradeHistoryRepository tradeHistoryRepository;


    public HoldingService(PortfolioItemRepository portfolioItemRepository, AssetCatalogRepository assetCatalogRepository, AssetPriceHistoryRepository priceHistoryRepository, PerformanceService performanceService, TradeHistoryRepository tradeHistoryRepository) {
        this.portfolioItemRepository = portfolioItemRepository;
        this.assetCatalogRepository = assetCatalogRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.performanceService = performanceService;
        this.tradeHistoryRepository = tradeHistoryRepository;
    }

    public List<HoldingView> getAllItems() {
        List<HoldingView> items = portfolioItemRepository.findAll();
        //performanceService.recordCurrentPortfolioValue();
        return items;
    }
    public HoldingView getItem(long id){
        return portfolioItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException
                        (HttpStatus.NOT_FOUND, "Portfolio item not found: " + id)
                );
    }

    private void validateBuyOrder(BuyRequest request) {
        if (request.getAssetCatalogId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "assetCatalogId is required");
        }
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be greater than zero");
        }
        if (request.getPriceTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "priceTime is required");
        }
    }

    public HoldingView createItem(BuyRequest request){
        validateBuyOrder(request);
        assetCatalogRepository.findById(request.getAssetCatalogId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Selected asset is not available"));

        var marketQuote = priceHistoryRepository.findByAssetIdAndTime(request.getAssetCatalogId(), request.getPriceTime())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected price time is not available"));

        PortfolioItem item = new PortfolioItem();
        item.setAssetCatalogId(request.getAssetCatalogId());
        item.setQuantity(request.getQuantity());
        item.setPurchasePrice(marketQuote.price());
        item.setPurchaseTime(marketQuote.priceTime());

        long id;
        var existingHolding = portfolioItemRepository.findByAssetCatalogId(item.getAssetCatalogId());
        if (existingHolding.isPresent()) {
            PortfolioItem existing = existingHolding.get();
            BigDecimal totalQuantity = existing.getQuantity().add(item.getQuantity());
            BigDecimal totalCost = existing.getQuantity().multiply(existing.getPurchasePrice())
                    .add(item.getQuantity().multiply(item.getPurchasePrice()));
            BigDecimal averagePrice = totalCost.divide(totalQuantity, 2, RoundingMode.HALF_UP);
            portfolioItemRepository.updateHolding(existing.getId(), totalQuantity, averagePrice, item.getPurchaseTime());
            id = existing.getId();
        } else {
            id = portfolioItemRepository.save(item);
        }
        tradeHistoryRepository.save(new TradeHistory(item.getAssetCatalogId(), "BUY", item.getQuantity(),
                marketQuote.price(), marketQuote.priceTime()));
        HoldingView result = getItem(id);
        //performanceService.recordCurrentPortfolioValue();
        return result;
    }

    public void sellItem(long id, SellRequest request) {
        HoldingView holding = getItem(id);
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sell quantity must be greater than zero");
        }
        if (request.getQuantity().compareTo(holding.getQuantity()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sell quantity cannot exceed current holding");
        }

        MarketAssetView marketAsset = priceHistoryRepository.findLatestByAssetId(holding.getAssetCatalogId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No market price is available"));
        BigDecimal remaining = holding.getQuantity().subtract(request.getQuantity());
        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            portfolioItemRepository.deleteById(id);
        } else {
            portfolioItemRepository.updateQuantity(id, remaining);
        }
        tradeHistoryRepository.save(new TradeHistory(holding.getAssetCatalogId(), "SELL", request.getQuantity(),
                marketAsset.getMarketPrice(), marketAsset.getPriceTime()));
        //performanceService.recordCurrentPortfolioValue();
    }

    public void deleteItem(long id) {
        HoldingView holding = getItem(id);
        SellRequest request = new SellRequest();
        request.setQuantity(holding.getQuantity());
        sellItem(id, request);
    }


}
