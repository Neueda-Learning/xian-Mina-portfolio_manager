-- Portfolio Manager 最终数据库基线：表结构、资产目录和真实 API 行情职责分离。

-- 可购买资产目录：只定义系统支持买卖什么，不代表用户已经持有。
CREATE TABLE IF NOT EXISTS asset_catalog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(10) NOT NULL UNIQUE,
    asset_name VARCHAR(100) NOT NULL,
    asset_type VARCHAR(20) NOT NULL
);

-- 当前持仓：同一资产只保留一行；多次买入后的数量和成本由 HoldingService 合并。
CREATE TABLE IF NOT EXISTS portfolio_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_catalog_id BIGINT NOT NULL,
    quantity DECIMAL(15, 4) NOT NULL,
    purchase_price DECIMAL(15, 2) NOT NULL,
    purchase_time DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_portfolio_asset (asset_catalog_id),
    CONSTRAINT fk_holding_asset FOREIGN KEY (asset_catalog_id)
        REFERENCES asset_catalog(id)
);

-- 外部 API 的五分钟行情；相同资产、相同时间只保存一次。
CREATE TABLE IF NOT EXISTS asset_price_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_catalog_id BIGINT NOT NULL,
    market_price DECIMAL(15, 2) NOT NULL,
    price_time DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_asset_market_time (asset_catalog_id, price_time),
    CONSTRAINT fk_market_price_asset FOREIGN KEY (asset_catalog_id)
        REFERENCES asset_catalog(id)
);

-- 每次买入或卖出都保留流水；即使当前持仓卖完，记录也不会删除。
CREATE TABLE IF NOT EXISTS trade_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_catalog_id BIGINT NOT NULL,
    trade_type VARCHAR(10) NOT NULL,
    quantity DECIMAL(15, 4) NOT NULL,
    trade_price DECIMAL(15, 2) NOT NULL,
    trade_time DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trade_asset FOREIGN KEY (asset_catalog_id)
        REFERENCES asset_catalog(id)
);

-- 组合每个行情时间点的总市值，后续用于 Dashboard 折线图。
CREATE TABLE IF NOT EXISTS portfolio_value_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    total_value DECIMAL(15, 2) NOT NULL,
    record_time DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_portfolio_value_time (record_time)
);

-- 培训价格 API 当前支持的五只股票。SQL 不写假价格，由 MarketPriceService 首次启动时同步。
INSERT INTO asset_catalog (ticker, asset_name, asset_type) VALUES
    ('AAPL', 'Apple Inc.', 'STOCK'),
    ('TSLA', 'Tesla Inc.', 'STOCK'),
    ('AMZN', 'Amazon.com Inc.', 'STOCK'),
    ('C', 'Citigroup Inc.', 'STOCK'),
    ('FB', 'Meta Platforms Inc.', 'STOCK'),
    ('CASH', 'Cash', 'CASH')
ON DUPLICATE KEY UPDATE asset_name = VALUES(asset_name), asset_type = VALUES(asset_type);
