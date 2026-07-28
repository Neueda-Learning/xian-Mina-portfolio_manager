package com.mina.minaportfoliomanagement.repository;

import com.mina.minaportfoliomanagement.dto.HoldingView;
import com.mina.minaportfoliomanagement.model.PortfolioItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class PortfolioItemRepository {
    private final JdbcTemplate jdbcTemplate;


    public PortfolioItemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private String selectHoldingSql() {
        //子查询取该资产最新的市场价格。
        return "SELECT p.id, p.asset_catalog_id, a.ticker, a.asset_name, a.asset_type, p.quantity, p.purchase_price, p.created_at, "
                + "latest_price.market_price FROM portfolio_item p JOIN asset_catalog a ON a.id = p.asset_catalog_id "
                + "JOIN asset_price_history latest_price ON latest_price.id = ("
                + "SELECT ph.id FROM asset_price_history ph WHERE ph.asset_catalog_id = a.id "
                + "ORDER BY ph.price_time DESC, ph.id DESC LIMIT 1)";
    }


    private HoldingView mapRow(long id, long assetCatalogId, String ticker, String assetName, String assetType,
                               BigDecimal quantity, BigDecimal purchasePrice, BigDecimal currentPrice,
                               java.time.LocalDateTime createdAt) {
        return new HoldingView(id, assetCatalogId, ticker, assetName, assetType, quantity, purchasePrice, currentPrice, createdAt);
    }

    public List<HoldingView> findAll() {
        String sql = selectHoldingSql() + " ORDER BY p.purchase_time DESC, p.id DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(
                rs.getLong("id"),
                rs.getLong("asset_catalog_id"),
                rs.getString("ticker"),
                rs.getString("asset_name"),
                rs.getString("asset_type"),
                rs.getBigDecimal("quantity"),
                rs.getBigDecimal("purchase_price"),
                rs.getBigDecimal("market_price"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ));
    }

    public Optional<HoldingView> findById(long id) {
        String sql = selectHoldingSql() + " WHERE p.id = ?";
        List<HoldingView> results = jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(
                rs.getLong("id"), rs.getLong("asset_catalog_id"), rs.getString("ticker"), rs.getString("asset_name"), rs.getString("asset_type"),
                rs.getBigDecimal("quantity"), rs.getBigDecimal("purchase_price"),
                rs.getBigDecimal("market_price"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), id);
        return results.stream().findFirst();
    }

    public Optional<PortfolioItem> findByAssetCatalogId(long assetCatalogId) {
        String sql = "SELECT id, asset_catalog_id, quantity, purchase_price, purchase_time, created_at "
                + "FROM portfolio_item WHERE asset_catalog_id = ?";
        List<PortfolioItem> results = jdbcTemplate.query(sql, (rs, rowNum) -> new PortfolioItem(
                rs.getLong("id"), rs.getLong("asset_catalog_id"),
                rs.getBigDecimal("quantity"), rs.getBigDecimal("purchase_price"),
                rs.getTimestamp("purchase_time").toLocalDateTime(),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), assetCatalogId);
        return results.stream().findFirst();
    }

    public long save(PortfolioItem item) {
        String sql = "INSERT INTO portfolio_item (asset_catalog_id, quantity, purchase_price, purchase_time) VALUES (?, ?, ?, ?)";
        // KeyHolder 可以取得本次买入的持仓 id；买入价格来自当天市场价格。
        org.springframework.jdbc.support.GeneratedKeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, item.getAssetCatalogId());
            ps.setBigDecimal(2, item.getQuantity());
            ps.setBigDecimal(3, item.getPurchasePrice());
            ps.setObject(4, item.getPurchaseTime());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key.longValue();
    }

    public int deleteById(long id) {
        return jdbcTemplate.update("DELETE FROM portfolio_item WHERE id = ?", id);
    }

    public int updateQuantity(long id, BigDecimal remainingQuantity) {
        return jdbcTemplate.update("UPDATE portfolio_item SET quantity = ? WHERE id = ?", remainingQuantity, id);
    }

    public int updateHolding(long id, BigDecimal quantity, BigDecimal averagePurchasePrice,
                             java.time.LocalDateTime latestPurchaseTime) {
        String sql = "UPDATE portfolio_item SET quantity = ?, purchase_price = ?, purchase_time = ? WHERE id = ?";
        return jdbcTemplate.update(sql, quantity, averagePurchasePrice, latestPurchaseTime, id);
    }



}
