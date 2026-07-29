package com.mina.minaportfoliomanagement.repository;

import com.mina.minaportfoliomanagement.model.PortfolioValueHistory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class PortfolioValueHistoryRepository {
    private final JdbcTemplate jdbcTemplate;

    public PortfolioValueHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveDailyValue(long portfolioId, BigDecimal totalValue) {
        saveValue(portfolioId, totalValue, LocalDateTime.now());
    }

    public void saveValue(long portfolioId, BigDecimal totalValue, LocalDateTime recordTime) {
        String sql = "INSERT INTO portfolio_value_history (portfolio_id, total_value, record_time) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE total_value = VALUES(total_value)";
        jdbcTemplate.update(sql, portfolioId, totalValue, recordTime);
    }

    public List<PortfolioValueHistory> findAll(long portfolioId) {
        String sql = "SELECT id, portfolio_id, total_value, record_time FROM portfolio_value_history "
                + "WHERE portfolio_id = ? ORDER BY record_time";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new PortfolioValueHistory(
                rs.getLong("id"), rs.getLong("portfolio_id"), rs.getBigDecimal("total_value"),
                rs.getTimestamp("record_time").toLocalDateTime()
        ), portfolioId);
    }
}
