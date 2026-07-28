package com.mina.minaportfoliomanagement.repository;

import com.mina.minaportfoliomanagement.dto.MarketAssetView;
import com.mina.minaportfoliomanagement.dto.MarketQuote;
import com.mina.minaportfoliomanagement.dto.PriceHistoryView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 负责保存和查询外部 API 的五分钟市场价格。 */
@Repository
public class AssetPriceHistoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public AssetPriceHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 查询一种资产的数据库最新价格；卖出时使用这一价格成交。 */
    public Optional<MarketAssetView> findLatestByAssetId(long assetCatalogId) {
        String sql = "SELECT a.id, a.ticker, a.asset_name, a.asset_type, p.market_price, p.price_time "
                + "FROM asset_catalog a JOIN asset_price_history p ON p.asset_catalog_id = a.id "
                + "WHERE a.id = ? ORDER BY p.price_time DESC, p.id DESC LIMIT 1";
        List<MarketAssetView> result = jdbcTemplate.query(sql, (resultSet, rowNum) -> new MarketAssetView(
                resultSet.getLong("id"),
                resultSet.getString("ticker"),
                resultSet.getString("asset_name"),
                resultSet.getString("asset_type"),
                resultSet.getBigDecimal("market_price"),
                resultSet.getTimestamp("price_time").toLocalDateTime()
        ), assetCatalogId);
        return result.stream().findFirst();
    }

    /** 组合绩效模块用最新行情时间作为价值快照的横轴。 */
    public LocalDateTime getLatestMarketTime() {
        return jdbcTemplate.queryForObject("SELECT MAX(price_time) FROM asset_price_history", LocalDateTime.class);
    }

    /** 同一资产和时间只保留一次；重复同步时更新价格而不是重复插入。 */
    public void savePrices(long assetCatalogId, List<MarketQuote> quotes) {
        String sql = "INSERT INTO asset_price_history (asset_catalog_id, market_price, price_time) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE market_price = VALUES(market_price)";
        for (MarketQuote quote : quotes) {
            jdbcTemplate.update(sql, assetCatalogId, quote.price(), quote.priceTime());
        }
    }

    /** 返回某只股票全部可选择的历史价格，最新时间排在最前。 */
    public List<PriceHistoryView> findAllByAssetId(long assetCatalogId) {
        String sql = "SELECT market_price, price_time FROM asset_price_history "
                + "WHERE asset_catalog_id = ? ORDER BY price_time DESC";
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new PriceHistoryView(
                resultSet.getBigDecimal("market_price"),
                resultSet.getTimestamp("price_time").toLocalDateTime()
        ), assetCatalogId);
    }

    /** 买入时按用户选择的时间从数据库查真实价格，不能相信客户端自己传来的价格。 */
    public Optional<MarketQuote> findByAssetIdAndTime(long assetCatalogId, LocalDateTime priceTime) {
        String sql = "SELECT market_price, price_time FROM asset_price_history "
                + "WHERE asset_catalog_id = ? AND price_time = ?";
        List<MarketQuote> result = jdbcTemplate.query(sql, (resultSet, rowNum) -> new MarketQuote(
                resultSet.getBigDecimal("market_price"),
                resultSet.getTimestamp("price_time").toLocalDateTime()
        ), assetCatalogId, priceTime);
        return result.stream().findFirst();
    }
}
