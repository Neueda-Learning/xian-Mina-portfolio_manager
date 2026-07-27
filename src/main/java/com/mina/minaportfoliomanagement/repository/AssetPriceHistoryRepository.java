package com.mina.minaportfoliomanagement.repository;

import com.mina.minaportfoliomanagement.dto.MarketAssetView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class AssetPriceHistoryRepository {
    private final JdbcTemplate jdbcTemplate;

    public AssetPriceHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    /**
     * 查询某一种资产的最新市场价格。
     * 新增持仓时会调用此方法，自动确定买入价。
     */
    public Optional<MarketAssetView> findLatestByAssetId(long assetCatalogId) {
        String sql = """
                SELECT a.id, a.ticker, a.asset_name, a.asset_type,
                       p.market_price, p.price_date
                FROM asset_catalog a
                JOIN asset_price_history p
                    ON p.asset_catalog_id = a.id
                WHERE a.id = ?
                ORDER BY p.price_date DESC, p.id DESC
                LIMIT 1
                """;

        List<MarketAssetView> result = jdbcTemplate.query(
                sql,
                (resultSet, rowNum) -> new MarketAssetView(
                        resultSet.getLong("id"),
                        resultSet.getString("ticker"),
                        resultSet.getString("asset_name"),
                        resultSet.getString("asset_type"),
                        resultSet.getBigDecimal("market_price"),
                        resultSet.getDate("price_date").toLocalDate()
                ),
                assetCatalogId
        );

        return result.stream().findFirst();
    }

    /**
     * 查询目前系统中最新的模拟交易日。
     */
    public LocalDate getLatestMarketDate() {
        String sql = "SELECT MAX(price_date) FROM asset_price_history";
        return jdbcTemplate.queryForObject(sql, LocalDate.class);
    }

    /**
     * 保存一种资产在某个模拟交易日的新价格。
     */
    public void savePrice(long assetCatalogId, BigDecimal marketPrice, LocalDate priceDate) {
        String sql = """
                INSERT INTO asset_price_history
                    (asset_catalog_id, market_price, price_date)
                VALUES (?, ?, ?)
                """;

        jdbcTemplate.update(sql, assetCatalogId, marketPrice, priceDate);
    }
}
