package com.mina.minaportfoliomanagement.repository;

import com.mina.minaportfoliomanagement.dto.MarketAssetView;
import com.mina.minaportfoliomanagement.model.AssetCatalog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** 负责读取可交易资产目录，以及初始化系统支持的加密货币。 */
@Repository
public class AssetCatalogRepository {

    private final JdbcTemplate jdbcTemplate;

    public AssetCatalogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 查询每只已有最新行情的可交易资产。 */
    public List<MarketAssetView> findAllWithLatestPrice() {
        String sql = "SELECT a.id, a.ticker, a.asset_name, a.asset_type, p.market_price, p.price_time "
                + "FROM asset_catalog a JOIN asset_price_history p ON p.id = ("
                + "SELECT latest.id FROM asset_price_history latest WHERE latest.asset_catalog_id = a.id "
                + "ORDER BY latest.price_time DESC, latest.id DESC LIMIT 1) "
                + "WHERE a.asset_type IN ('STOCK', 'CRYPTO') ORDER BY a.asset_type, a.ticker";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new MarketAssetView(
                resultSet.getLong("id"),
                resultSet.getString("ticker"),
                resultSet.getString("asset_name"),
                resultSet.getString("asset_type"),
                resultSet.getBigDecimal("market_price"),
                resultSet.getTimestamp("price_time").toLocalDateTime()
        ));
    }

    /** 查询培训提供的股票 API 支持的全部股票。 */
    public List<AssetCatalog> findAllStocks() {
        String sql = "SELECT id, ticker, asset_name, asset_type FROM asset_catalog "
                + "WHERE asset_type = 'STOCK' ORDER BY ticker";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new AssetCatalog(
                resultSet.getLong("id"),
                resultSet.getString("ticker"),
                resultSet.getString("asset_name"),
                resultSet.getString("asset_type")
        ));
    }

    /** 查询由 CoinGecko 同步行情的加密货币。 */
    public List<AssetCatalog> findAllCrypto() {
        String sql = "SELECT id, ticker, asset_name, asset_type FROM asset_catalog "
                + "WHERE asset_type = 'CRYPTO' ORDER BY ticker";

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> new AssetCatalog(
                resultSet.getLong("id"),
                resultSet.getString("ticker"),
                resultSet.getString("asset_name"),
                resultSet.getString("asset_type")
        ));
    }

    /** 为已运行过的旧数据库补充 BTC、ETH，不要求用户手工清库。 */
    public void ensureCryptoAssets() {
        String sql = "INSERT INTO asset_catalog (ticker, asset_name, asset_type) VALUES "
                + "('BTC', 'Bitcoin', 'CRYPTO'), ('ETH', 'Ethereum', 'CRYPTO') "
                + "ON DUPLICATE KEY UPDATE asset_name = VALUES(asset_name), asset_type = VALUES(asset_type)";
        jdbcTemplate.update(sql);
    }

    /** 按 id 校验资产存在；持仓模块买入前会调用它。 */
    public Optional<AssetCatalog> findById(long id) {
        String sql = "SELECT id, ticker, asset_name, asset_type FROM asset_catalog "
                + "WHERE id = ?";
        List<AssetCatalog> result = jdbcTemplate.query(sql, (resultSet, rowNum) -> new AssetCatalog(
                resultSet.getLong("id"),
                resultSet.getString("ticker"),
                resultSet.getString("asset_name"),
                resultSet.getString("asset_type")
        ), id);
        return result.stream().findFirst();
    }

    public Optional<AssetCatalog> findByTicker(String ticker) {
        String sql = "SELECT id, ticker, asset_name, asset_type FROM asset_catalog WHERE ticker = ?";
        List<AssetCatalog> result = jdbcTemplate.query(sql, (resultSet, rowNum) -> new AssetCatalog(
                resultSet.getLong("id"),
                resultSet.getString("ticker"),
                resultSet.getString("asset_name"),
                resultSet.getString("asset_type")
        ), ticker);
        return result.stream().findFirst();
    }

    public AssetCatalog ensureCashAsset() {
        return findByTicker("CASH").orElseGet(() -> {
            org.springframework.jdbc.support.GeneratedKeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                java.sql.PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO asset_catalog (ticker, asset_name, asset_type) VALUES (?, ?, ?)",
                        new String[]{"id"}
                );
                ps.setString(1, "CASH");
                ps.setString(2, "Cash");
                ps.setString(3, "CASH");
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            return new AssetCatalog(key.longValue(), "CASH", "Cash", "CASH");
        });
    }
}
