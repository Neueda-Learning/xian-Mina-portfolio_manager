package com.mina.minaportfoliomanagement.service;

import com.mina.minaportfoliomanagement.dto.BuyRequest;
import com.mina.minaportfoliomanagement.dto.CashDepositRequest;
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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class HoldingService {
    private final PortfolioItemRepository portfolioItemRepository;
    private final AssetCatalogRepository assetCatalogRepository;
    private final AssetPriceHistoryRepository priceHistoryRepository;
    private final PerformanceService performanceService;
    private final TradeHistoryRepository tradeHistoryRepository;
    private final PortfolioService portfolioService;


    public HoldingService(PortfolioItemRepository portfolioItemRepository, AssetCatalogRepository assetCatalogRepository,
                          AssetPriceHistoryRepository priceHistoryRepository, PerformanceService performanceService,
                          TradeHistoryRepository tradeHistoryRepository, PortfolioService portfolioService) {
        this.portfolioItemRepository = portfolioItemRepository;
        this.assetCatalogRepository = assetCatalogRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.performanceService = performanceService;
        this.tradeHistoryRepository = tradeHistoryRepository;
        this.portfolioService = portfolioService;
    }

    public List<HoldingView> getAllItems(Long portfolioId) {
        long resolvedPortfolioId = portfolioService.requirePortfolioId(portfolioId);
        List<HoldingView> items = portfolioItemRepository.findAll(resolvedPortfolioId);
        performanceService.recordCurrentPortfolioValue(resolvedPortfolioId);
        return items;
    }

    public HoldingView getItem(long id, long portfolioId) {
        return portfolioItemRepository.findById(id, portfolioId)
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
        long portfolioId = portfolioService.requirePortfolioId(request.getPortfolioId());
        assetCatalogRepository.findById(request.getAssetCatalogId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Selected asset is not available"));

        var marketQuote = priceHistoryRepository.findByAssetIdAndTime(request.getAssetCatalogId(), request.getPriceTime())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected price time is not available"));

        PortfolioItem item = new PortfolioItem();
        item.setPortfolioId(portfolioId);
        item.setAssetCatalogId(request.getAssetCatalogId());
        item.setQuantity(request.getQuantity());
        item.setPurchasePrice(marketQuote.price());
        item.setPurchaseTime(marketQuote.priceTime());

        long id;
        var existingHolding = portfolioItemRepository.findByAssetCatalogId(portfolioId, item.getAssetCatalogId());
        if (existingHolding.isPresent()) {
            PortfolioItem existing = existingHolding.get();
            BigDecimal totalQuantity = existing.getQuantity().add(item.getQuantity());
            BigDecimal totalCost = existing.getQuantity().multiply(existing.getPurchasePrice())
                    .add(item.getQuantity().multiply(item.getPurchasePrice()));
            BigDecimal averagePrice = totalCost.divide(totalQuantity, 2, RoundingMode.HALF_UP);
            portfolioItemRepository.updateHolding(existing.getId(), portfolioId, totalQuantity, averagePrice, item.getPurchaseTime());
            id = existing.getId();
        } else {
            id = portfolioItemRepository.save(item);
        }
        tradeHistoryRepository.save(new TradeHistory(portfolioId, item.getAssetCatalogId(), "BUY", item.getQuantity(),
                marketQuote.price(), marketQuote.priceTime()));
        HoldingView result = getItem(id, portfolioId);
        performanceService.recordCurrentPortfolioValue(portfolioId);
        return result;
    }

    public void sellItem(long id, Long portfolioId, SellRequest request) {
        long resolvedPortfolioId = portfolioService.requirePortfolioId(portfolioId);
        HoldingView holding = getItem(id, resolvedPortfolioId);
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
            portfolioItemRepository.deleteById(id, resolvedPortfolioId);
        } else {
            portfolioItemRepository.updateQuantity(id, resolvedPortfolioId, remaining);
        }
        tradeHistoryRepository.save(new TradeHistory(resolvedPortfolioId, holding.getAssetCatalogId(), "SELL", request.getQuantity(),
                marketAsset.getMarketPrice(), marketAsset.getPriceTime()));
        performanceService.recordCurrentPortfolioValue(resolvedPortfolioId);
    }

    public void deleteItem(long id, Long portfolioId) {
        long resolvedPortfolioId = portfolioService.requirePortfolioId(portfolioId);
        HoldingView holding = getItem(id, resolvedPortfolioId);
        SellRequest request = new SellRequest();
        request.setQuantity(holding.getQuantity());
        sellItem(id, resolvedPortfolioId, request);
    }


}
