package com.mina.minaportfoliomanagement.repository;

import com.mina.minaportfoliomanagement.model.Portfolio;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 管理投资组合，并在首次启动时将旧的单组合数据迁移到默认组合。 */
@Repository
public class PortfolioRepository {

    public static final long DEFAULT_PORTFOLIO_ID = 1L;
    private final JdbcTemplate jdbcTemplate;

    public PortfolioRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 兼容已存在的 portfolioDb：不删除旧数据，只补充 portfolio_id。 */
    @PostConstruct
    public void initializePortfolioSchema() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS portfolio ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "portfolio_name VARCHAR(100) NOT NULL UNIQUE, "
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO portfolio (id, portfolio_name) VALUES (?, ?) "
                        + "ON DUPLICATE KEY UPDATE portfolio_name = portfolio_name",
                DEFAULT_PORTFOLIO_ID, "My Portfolio");

        addPortfolioId("portfolio_item");
        addPortfolioId("trade_history");
        addPortfolioId("portfolio_value_history");

        replaceHoldingIndex();
        replaceIndex("portfolio_value_history", "uk_portfolio_value_time", "portfolio_id, record_time");
    }

    private void addPortfolioId(String tableName) {
        if (!columnExists(tableName, "portfolio_id")) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN portfolio_id BIGINT NULL AFTER id");
        }
        jdbcTemplate.update("UPDATE " + tableName + " SET portfolio_id = ? WHERE portfolio_id IS NULL", DEFAULT_PORTFOLIO_ID);
        jdbcTemplate.execute("ALTER TABLE " + tableName + " MODIFY COLUMN portfolio_id BIGINT NOT NULL");
    }

    /** MySQL 版本差异较大，因此不使用 ADD COLUMN IF NOT EXISTS 语法。 */
    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                Integer.class, tableName, indexName);
        return count != null && count > 0;
    }

    private boolean foreignKeyExists(String tableName, String constraintName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints "
                        + "WHERE table_schema = DATABASE() AND table_name = ? "
                        + "AND constraint_name = ? AND constraint_type = 'FOREIGN KEY'",
                Integer.class, tableName, constraintName);
        return count != null && count > 0;
    }

    private boolean indexMatches(String tableName, String indexName, String expectedColumns) {
        if (!indexExists(tableName, indexName)) {
            return false;
        }
        String columns = jdbcTemplate.queryForObject(
                "SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',') "
                        + "FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                String.class, tableName, indexName);
        return expectedColumns.equalsIgnoreCase(columns);
    }

    /** 将旧的“一个资产全库只能有一条持仓”索引改为“每个组合各一条”。 */
    private void replaceHoldingIndex() {
        if (indexMatches("portfolio_item", "uk_portfolio_asset", "portfolio_id,asset_catalog_id")) {
            return;
        }
        if (foreignKeyExists("portfolio_item", "fk_holding_asset")) {
            jdbcTemplate.execute("ALTER TABLE portfolio_item DROP FOREIGN KEY fk_holding_asset");
        }
        if (indexExists("portfolio_item", "uk_portfolio_asset")) {
            jdbcTemplate.execute("ALTER TABLE portfolio_item DROP INDEX uk_portfolio_asset");
        }
        jdbcTemplate.execute("ALTER TABLE portfolio_item ADD UNIQUE INDEX uk_portfolio_asset "
                + "(portfolio_id, asset_catalog_id)");
        if (!indexExists("portfolio_item", "idx_holding_asset")) {
            jdbcTemplate.execute("ALTER TABLE portfolio_item ADD INDEX idx_holding_asset (asset_catalog_id)");
        }
        jdbcTemplate.execute("ALTER TABLE portfolio_item ADD CONSTRAINT fk_holding_asset "
                + "FOREIGN KEY (asset_catalog_id) REFERENCES asset_catalog(id)");
    }

    /** 旧唯一索引不含 portfolio_id，需要删除后按组合重建。 */
    private void replaceIndex(String tableName, String indexName, String columns) {
        String expectedColumns = columns.replace(" ", "");
        if (indexMatches(tableName, indexName, expectedColumns)) {
            return;
        }
        if (indexExists(tableName, indexName)) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP INDEX " + indexName);
        }
        jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD UNIQUE INDEX " + indexName + " (" + columns + ")");
    }

    public List<Portfolio> findAll() {
        String sql = "SELECT id, portfolio_name, created_at FROM portfolio ORDER BY id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new Portfolio(
                rs.getLong("id"), rs.getString("portfolio_name"),
                rs.getTimestamp("created_at").toLocalDateTime()));
    }

    public Optional<Portfolio> findById(long id) {
        String sql = "SELECT id, portfolio_name, created_at FROM portfolio WHERE id = ?";
        List<Portfolio> result = jdbcTemplate.query(sql, (rs, rowNum) -> new Portfolio(
                rs.getLong("id"), rs.getString("portfolio_name"),
                rs.getTimestamp("created_at").toLocalDateTime()), id);
        return result.stream().findFirst();
    }

    public Portfolio save(String portfolioName) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO portfolio (portfolio_name) VALUES (?)", new String[]{"id"});
            statement.setString(1, portfolioName);
            return statement;
        }, keyHolder);
        long id = keyHolder.getKey().longValue();
        return new Portfolio(id, portfolioName, LocalDateTime.now());
    }
}
