-- Portfolio Manager 数据库初始化：建表和第一天演示市场数据都集中在此文件。

-- 可购买资产目录：不代表用户已持有，只定义系统支持买卖什么资产。
CREATE TABLE IF NOT EXISTS asset_catalog (
                                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             ticker VARCHAR(10) NOT NULL UNIQUE,
    asset_name VARCHAR(100) NOT NULL,
    asset_type VARCHAR(20) NOT NULL
    );

-- 用户持仓：每一行是一笔在某个模拟交易日买入的资产批次。
CREATE TABLE IF NOT EXISTS portfolio_item (
                                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              asset_catalog_id BIGINT NOT NULL,
                                              quantity DECIMAL(15, 4) NOT NULL,
    purchase_price DECIMAL(15, 2) NOT NULL,
    purchase_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_holding_asset FOREIGN KEY (asset_catalog_id)
    REFERENCES asset_catalog(id)
    );

-- 市场价格历史：同一资产、同一天只有一个市场价格。
CREATE TABLE IF NOT EXISTS asset_price_history (
                                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                   asset_catalog_id BIGINT NOT NULL,
                                                   market_price DECIMAL(15, 2) NOT NULL,
    price_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_asset_market_day (asset_catalog_id, price_date),
    CONSTRAINT fk_market_price_asset FOREIGN KEY (asset_catalog_id)
    REFERENCES asset_catalog(id)
    );

-- 组合每日总市值：供 Dashboard 的绩效曲线读取。
CREATE TABLE IF NOT EXISTS portfolio_value_history (
                                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                       total_value DECIMAL(15, 2) NOT NULL,
    record_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_portfolio_value_day (record_date)
    );

-- 第一部分演示数据：可购买资产目录。此时没有任何用户持仓。
INSERT INTO asset_catalog (ticker, asset_name, asset_type) VALUES
                                                               ('AAPL', 'Apple Inc.', 'STOCK'),
                                                               ('TSLA', 'Tesla Inc.', 'STOCK'),
                                                               ('AMZN', 'Amazon.com Inc.', 'STOCK'),
                                                               ('MSFT', 'Microsoft Corp.', 'STOCK'),
                                                               ('BND', 'Bond Fund', 'BOND'),
                                                               ('BTC', 'Bitcoin', 'CRYPTO'),
                                                               ('ETH', 'Ethereum', 'CRYPTO'),
                                                               ('USD', 'Cash Balance', 'CASH')
    ON DUPLICATE KEY UPDATE asset_name = VALUES(asset_name), asset_type = VALUES(asset_type);

-- 第一模拟交易日（2026-07-01）的市场价格。用户可从这些资产中选择购买。
INSERT INTO asset_price_history (asset_catalog_id, market_price, price_date)
SELECT id,
       CASE ticker
           WHEN 'AAPL' THEN 180.00
           WHEN 'TSLA' THEN 235.00
           WHEN 'AMZN' THEN 170.00
           WHEN 'MSFT' THEN 400.00
           WHEN 'BND' THEN 76.00
           WHEN 'BTC' THEN 62000.00
           WHEN 'ETH' THEN 3000.00
           WHEN 'USD' THEN 1.00
           END,
       '2026-07-01'
FROM asset_catalog
    ON DUPLICATE KEY UPDATE market_price = VALUES(market_price);

-- 第一日尚未买入任何资产，因此组合总市值为零。
INSERT INTO portfolio_value_history (total_value, record_date) VALUES (0.00, '2026-07-01')
    ON DUPLICATE KEY UPDATE total_value = total_value;
