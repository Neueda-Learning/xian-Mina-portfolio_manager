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
    public PerformanceService(PerformanceRepository performanceRepository,
                              AssetPriceHistoryRepository priceHistoryRepository,
                              PortfolioValueHistoryRepository valueHistoryRepository) {
        this.performanceRepository = performanceRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.valueHistoryRepository = valueHistoryRepository;
    }

    /** 按当前最新市场价格记录组合市值。 */
    public void recordCurrentPortfolioValue() {
        BigDecimal totalValue = performanceRepository.calculateCurrentPortfolioValue();
        LocalDateTime marketTime = priceHistoryRepository.getLatestMarketTime();
        // 没有价格数据时不记录，正常启动后市场模块会先完成首次价格同步。
        if (marketTime != null) {
            valueHistoryRepository.saveValue(totalValue, marketTime);
        }
    }

    public List<PortfolioValueHistory> getPerformance() {
        recordCurrentPortfolioValue();
        return valueHistoryRepository.findAll();
    }



}
