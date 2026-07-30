package com.mina.minaportfoliomanagement.service;

import com.mina.minaportfoliomanagement.model.PortfolioValueHistory;
import com.mina.minaportfoliomanagement.repository.AssetPriceHistoryRepository;
import com.mina.minaportfoliomanagement.repository.PerformanceRepository;
import com.mina.minaportfoliomanagement.repository.PortfolioValueHistoryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PerformanceService {
    private final PerformanceRepository performanceRepository;
    private final AssetPriceHistoryRepository priceHistoryRepository;
    private final PortfolioValueHistoryRepository valueHistoryRepository;
    private final PortfolioService portfolioService;
    public PerformanceService(PerformanceRepository performanceRepository,
                              AssetPriceHistoryRepository priceHistoryRepository,
                              PortfolioValueHistoryRepository valueHistoryRepository,
                              PortfolioService portfolioService) {
        this.performanceRepository = performanceRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.valueHistoryRepository = valueHistoryRepository;
        this.portfolioService = portfolioService;
    }

    /** 按当前最新市场价格记录组合市值。 */
    public void recordCurrentPortfolioValue(long portfolioId) {
        BigDecimal totalValue = performanceRepository.calculateCurrentPortfolioValue(portfolioId);
        LocalDateTime marketTime = priceHistoryRepository.getLatestMarketTime();
        // 没有价格数据时不记录，正常启动后市场模块会先完成首次价格同步。
        if (marketTime != null) {
            valueHistoryRepository.saveValue(portfolioId, totalValue, marketTime);
        }
    }

    /** 市场价格刷新后，为每一个组合分别保存一条市值快照。 */
    public void recordAllPortfoliosValue() {
        portfolioService.getAllPortfolios().forEach(portfolio -> recordCurrentPortfolioValue(portfolio.getId()));
    }

    public List<PortfolioValueHistory> getPerformance(Long portfolioId) {
        long resolvedPortfolioId = portfolioService.requirePortfolioId(portfolioId);
        recordCurrentPortfolioValue(resolvedPortfolioId);
        return valueHistoryRepository.findAll(resolvedPortfolioId);
    }



}
