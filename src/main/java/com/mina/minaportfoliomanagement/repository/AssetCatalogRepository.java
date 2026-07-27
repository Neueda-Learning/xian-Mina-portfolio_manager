package com.mina.minaportfoliomanagement.repository;

import com.mina.minaportfoliomanagement.dto.MarketAssetView;
import com.mina.minaportfoliomanagement.model.AssetCatalog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AssetCatalogRepository {
    private final JdbcTemplate jdbcTemplate;

    public AssetCatalogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    public List<MarketAssetView> findAllWithLatestPrice() {
        String sql = "SELECT a.id, a.ticker, a.asset_name, a.asset_type, "
                + "p.market_price, p.price_date "
                + "FROM asset_catalog a "
                + "JOIN asset_price_history p ON p.id = ("
                + "SELECT latest.id "
                + "FROM asset_price_history latest "
                + "WHERE latest.asset_catalog_id = a.id "
                + "ORDER BY latest.price_date DESC, latest.id DESC "
                + "LIMIT 1"
                + ") "
                + "ORDER BY a.ticker";

        return jdbcTemplate.query(sql, (resultSet, rowNum) ->
                new MarketAssetView(
                        resultSet.getLong("id"),
                        resultSet.getString("ticker"),
                        resultSet.getString("asset_name"),
                        resultSet.getString("asset_type"),
                        resultSet.getBigDecimal("market_price"),
                        resultSet.getDate("price_date").toLocalDate()
                )
        );
    }
    /**
     * 按资产 id 查询资产。
     * 后续新增持仓前，后端会用它校验该资产是否存在。
     */
    public Optional<AssetCatalog> findById(long id) {
        String sql = """
                SELECT id, ticker, asset_name, asset_type
                FROM asset_catalog
                WHERE id = ?
                """;

        List<AssetCatalog> result = jdbcTemplate.query(sql, (resultSet, rowNum) ->
                        new AssetCatalog(
                                resultSet.getLong("id"),
                                resultSet.getString("ticker"),
                                resultSet.getString("asset_name"),
                                resultSet.getString("asset_type")
                        ), id );

        return result.stream().findFirst();
    }
}
