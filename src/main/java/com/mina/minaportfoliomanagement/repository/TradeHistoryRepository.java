package com.mina.minaportfoliomanagement.repository;

import com.mina.minaportfoliomanagement.model.TradeHistory;
import org.springframework.jdbc.core.JdbcTemplate;

public class TradeHistoryRepository {
    private final JdbcTemplate jdbcTemplate;

    public TradeHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(TradeHistory trade) {
        String sql = "INSERT INTO trade_history "
                + "(asset_catalog_id, trade_type, quantity, trade_price, trade_time) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, trade.getAssetCatalogId(), trade.getTradeType(),
                trade.getQuantity(), trade.getTradePrice(), trade.getTradeTime());
    }
}
