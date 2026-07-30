package com.mina.minaportfoliomanagement.repository;

import com.mina.minaportfoliomanagement.model.TradeHistory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TradeHistoryRepository {
    private final JdbcTemplate jdbcTemplate;

    /**
     * Creates the repository with JdbcTemplate for trade history persistence.
     */
    public TradeHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Inserts a trade record into the trade history table.
     */
    public void save(TradeHistory trade) {
        String sql = "INSERT INTO trade_history "
                + "(portfolio_id, asset_catalog_id, trade_type, quantity, trade_price, trade_time) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, trade.getPortfolioId(), trade.getAssetCatalogId(), trade.getTradeType(),
                trade.getQuantity(), trade.getTradePrice(), trade.getTradeTime());
    }
}
