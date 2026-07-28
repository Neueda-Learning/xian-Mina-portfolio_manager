package com.mina.minaportfoliomanagement.service;

import com.mina.minaportfoliomanagement.dto.HoldingView;
import com.mina.minaportfoliomanagement.dto.PortfolioSummary;
import com.mina.minaportfoliomanagement.repository.PortfolioItemRepository;
import com.mina.minaportfoliomanagement.repository.SummaryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class SummaryService {
    private final SummaryRepository summaryRepository;
    private final PortfolioItemRepository portfolioItemRepository;

    public SummaryService(SummaryRepository summaryRepository,
                          PortfolioItemRepository portfolioItemRepository) {
        this.summaryRepository = summaryRepository;
        this.portfolioItemRepository = portfolioItemRepository;
    }

    public PortfolioSummary getSummary() {
        PortfolioSummary summary = summaryRepository.calculateSummary();
        List<HoldingView> items = portfolioItemRepository.findAll();

        summary.setItemCount(items.size());
        summary.setItems(items);

        BigDecimal totalReturn = summary.getTotalMarketValue().subtract(summary.getTotalCost());
        summary.setTotalReturn(totalReturn);

        if (summary.getTotalCost().compareTo(BigDecimal.ZERO) == 0) {
            summary.setReturnPercent(BigDecimal.ZERO);
        } else {
            BigDecimal percent = totalReturn.divide(summary.getTotalCost(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            summary.setReturnPercent(percent);
        }

        return summary;
    }


}
