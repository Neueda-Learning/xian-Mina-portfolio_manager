package com.mina.minaportfoliomanagement.repository;

import com.mina.minaportfoliomanagement.dto.PortfolioSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SummaryRepository {
    private final JdbcTemplate jdbcTemplate;

    public SummaryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PortfolioSummary calculateSummary() {
        String sql = "SELECT COALESCE(SUM(p.quantity * p.purchase_price), 0) AS total_cost, "
                + "COALESCE(SUM(p.quantity * latest_price.market_price), 0) AS total_market_value "
                + "FROM portfolio_item p "
                + "JOIN asset_price_history latest_price ON latest_price.id = ("
                + "SELECT ph.id FROM asset_price_history ph WHERE ph.asset_catalog_id = p.asset_catalog_id "
                + "ORDER BY ph.price_time DESC, ph.id DESC LIMIT 1)";

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            PortfolioSummary summary = new PortfolioSummary();
            summary.setTotalCost(rs.getBigDecimal("total_cost"));
            summary.setTotalMarketValue(rs.getBigDecimal("total_market_value"));
            return summary;
        });
    }

}
