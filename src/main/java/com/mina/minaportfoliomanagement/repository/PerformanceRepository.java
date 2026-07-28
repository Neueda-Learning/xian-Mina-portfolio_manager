package com.mina.minaportfoliomanagement.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class PerformanceRepository {
    private final JdbcTemplate jdbcTemplate;

    public PerformanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    public BigDecimal calculateCurrentPortfolioValue() {
        String sql = "SELECT COALESCE(SUM(p.quantity * latest_price.market_price), 0) AS total_market_value "
                + "FROM portfolio_item p "
                + "JOIN asset_price_history latest_price ON latest_price.id = ("
                + "SELECT ph.id FROM asset_price_history ph WHERE ph.asset_catalog_id = p.asset_catalog_id "
                + "ORDER BY ph.price_time DESC, ph.id DESC LIMIT 1)";
        return jdbcTemplate.queryForObject(sql, BigDecimal.class);
    }
}
