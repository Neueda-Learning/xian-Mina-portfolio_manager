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
    private record SellQuote(BigDecimal price, LocalDateTime time) {}

    private final PortfolioItemRepository portfolioItemRepository;
    private final AssetCatalogRepository assetCatalogRepository;
    private final AssetPriceHistoryRepository priceHistoryRepository;
    private final PerformanceService performanceService;
    private final TradeHistoryRepository tradeHistoryRepository;
    private final PortfolioService portfolioService;
    private final CashFxService cashFxService;

    /**
     * Creates the holding service with all repositories and domain services required
     * for buy, sell, and cash deposit workflows.
     */
    public HoldingService(PortfolioItemRepository portfolioItemRepository, AssetCatalogRepository assetCatalogRepository,
                          AssetPriceHistoryRepository priceHistoryRepository, PerformanceService performanceService,
                          TradeHistoryRepository tradeHistoryRepository, PortfolioService portfolioService,
                          CashFxService cashFxService) {
        this.portfolioItemRepository = portfolioItemRepository;
        this.assetCatalogRepository = assetCatalogRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.performanceService = performanceService;
        this.tradeHistoryRepository = tradeHistoryRepository;
        this.portfolioService = portfolioService;
        this.cashFxService = cashFxService;
    }

    /**
     * Returns all holdings for the requested portfolio and records a latest portfolio
     * valuation snapshot.
     */
    public List<HoldingView> getAllItems(Long portfolioId) {
        long resolvedPortfolioId = portfolioService.requirePortfolioId(portfolioId);
        List<HoldingView> items = portfolioItemRepository.findAll(resolvedPortfolioId);
        performanceService.recordCurrentPortfolioValue(resolvedPortfolioId);
        return items;
    }

    /**
     * Returns a single holding by id within the given portfolio.
     * Throws 404 when the holding does not exist.
     */
    public HoldingView getItem(long id, long portfolioId) {
        return portfolioItemRepository.findById(id, portfolioId)
                .orElseThrow(() -> new ResponseStatusException
                        (HttpStatus.NOT_FOUND, "Portfolio item not found: " + id)
                );
    }

    /**
     * Validates buy order inputs before market quote lookup and persistence.
     */
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

    /**
     * Creates a new holding from a buy request, or merges into an existing holding
     * by recalculating weighted average purchase price.
     */
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
        tradeHistoryRepository.save(new TradeHistory(item.getAssetCatalogId(), portfolioId, "BUY", item.getQuantity(),
                marketQuote.price(), marketQuote.priceTime()));
        HoldingView result = getItem(id, portfolioId);
        performanceService.recordCurrentPortfolioValue(portfolioId);
        return result;
    }

    /**
     * Deposits cash in a selected currency into the portfolio. Existing cash holdings
     * are merged using a weighted average FX rate in USD terms.
     */
    public HoldingView addCash(CashDepositRequest request) {
        long portfolioId = portfolioService.requirePortfolioId(request.getPortfolioId());
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be greater than zero");
        }

        CashFxService.FxQuote fxQuote = cashFxService.getUsdQuote(request.resolveCurrencyCode());
        var cashAsset = assetCatalogRepository.ensureCashAsset(fxQuote.currencyCode());
        BigDecimal amount = request.getAmount().setScale(4, RoundingMode.HALF_UP);
        LocalDateTime now = LocalDateTime.now();

        PortfolioItem item = new PortfolioItem();
        item.setPortfolioId(portfolioId);
        item.setAssetCatalogId(cashAsset.getId());
        item.setQuantity(amount);
        item.setPurchasePrice(fxQuote.usdRate());
        item.setPurchaseTime(now);

        long id;
        var existingHolding = portfolioItemRepository.findByAssetCatalogId(portfolioId, item.getAssetCatalogId());
        if (existingHolding.isPresent()) {
            PortfolioItem existing = existingHolding.get();
            BigDecimal totalQuantity = existing.getQuantity().add(item.getQuantity());
            BigDecimal totalUsdCost = existing.getQuantity().multiply(existing.getPurchasePrice())
                    .add(item.getQuantity().multiply(item.getPurchasePrice()));
            BigDecimal averageFxRate = totalUsdCost.divide(totalQuantity, 6, RoundingMode.HALF_UP);
            portfolioItemRepository.updateHolding(existing.getId(), portfolioId, totalQuantity, averageFxRate, now);
            id = existing.getId();
        } else {
            id = portfolioItemRepository.save(item);
        }

        tradeHistoryRepository.save(new TradeHistory(item.getAssetCatalogId(), portfolioId, "DEPOSIT", item.getQuantity(),
                item.getPurchasePrice(), now));
        HoldingView result = getItem(id, portfolioId);
        performanceService.recordCurrentPortfolioValue(portfolioId);
        return result;
    }

    /**
     * Sells part or all of a holding and records the trade with the resolved sell quote.
     */
    public void sellItem(long id, Long portfolioId, SellRequest request) {
        long resolvedPortfolioId = portfolioService.requirePortfolioId(portfolioId);
        HoldingView holding = getItem(id, resolvedPortfolioId);
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sell quantity must be greater than zero");
        }
        if (request.getQuantity().compareTo(holding.getQuantity()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sell quantity cannot exceed current holding");
        }

        SellQuote sellQuote = resolveSellQuote(holding);
        BigDecimal remaining = holding.getQuantity().subtract(request.getQuantity());
        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            portfolioItemRepository.deleteById(id, resolvedPortfolioId);
        } else {
            portfolioItemRepository.updateQuantity(id, resolvedPortfolioId, remaining);
        }
        tradeHistoryRepository.save(new TradeHistory(holding.getAssetCatalogId(), resolvedPortfolioId, "SELL",
                request.getQuantity(), sellQuote.price(), sellQuote.time()));
        performanceService.recordCurrentPortfolioValue(resolvedPortfolioId);
    }

    /**
     * Removes a holding by selling its full remaining quantity.
     */
    public void deleteItem(long id, Long portfolioId) {
        long resolvedPortfolioId = portfolioService.requirePortfolioId(portfolioId);
        HoldingView holding = getItem(id, resolvedPortfolioId);
        SellRequest request = new SellRequest();
        request.setQuantity(holding.getQuantity());
        sellItem(id, resolvedPortfolioId, request);
    }

    /**
     * Resolves the price and time to use for a sell trade.
     * Cash holdings use their stored purchase price; other assets use latest market price.
     */
    private SellQuote resolveSellQuote(HoldingView holding) {
        if (isCashHolding(holding)) {
            return new SellQuote(holding.getPurchasePrice(), LocalDateTime.now());
        }
        MarketAssetView marketAsset = priceHistoryRepository.findLatestByAssetId(holding.getAssetCatalogId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No market price is available"));
        return new SellQuote(marketAsset.getMarketPrice(), marketAsset.getPriceTime());
    }

    /**
     * Returns true when the holding is a cash asset.
     */
    private boolean isCashHolding(HoldingView holding) {
        return "CASH".equalsIgnoreCase(holding.getAssetType());
    }




}
