# Add Cash 全流程说明（前端 -> 后端 -> 外部 REST API -> 数据库）

## 1. 功能目标

`Add Cash` 用于向投资组合追加现金，支持多币种（`USD / GBP / EUR / CNY / JPY / HKD`）。

系统会将该币种金额按实时汇率折算为 USD 计价（用于组合估值），并将不同币种分别保存为独立资产（例如 `CASH_USD`、`CASH_EUR`）。

---

## 2. 入口与接口定义

### 前端入口
- 按钮：`+ Add Cash`
- 表单字段：
  - `cashAmount`（金额）
  - `cashCurrency`（币种）

### 后端 REST 接口
- **URL**: `POST /api/portfolio-items/cash`
- **Controller**: `HoldingController.addCash(@RequestBody CashDepositRequest request)`
- **返回**: `201 Created` + `HoldingView`

### 请求示例
```json
{
  "amount": 444,
  "currencyCode": "EUR"
}
```

---

## 3. 核心调用链（按执行顺序）

### 3.1 前端发起请求

文件：`src/main/resources/static/app.js`  
函数：`saveCash(event)`

关键动作：
1. 从 `FormData` 读取 `cashAmount` 和 `cashCurrency`。
2. 校验金额必须大于 0，币种不能为空。
3. 调用：
   ```js
   fetch("/api/portfolio-items/cash", {
     method: "POST",
     headers: { "Content-Type": "application/json" },
     body: JSON.stringify({ amount, currencyCode })
   })
   ```

---

### 3.2 Controller 接收

文件：`src/main/java/com/mina/minaportfoliomanagement/controller/HoldingController.java`  
函数：`addCash(CashDepositRequest request)`

关键动作：
1. 接收 JSON 并绑定到 `CashDepositRequest`。
2. 调用 `holdingService.addCash(request)`。
3. 返回 `201 Created`。

---

### 3.3 Service 业务主流程

文件：`src/main/java/com/mina/minaportfoliomanagement/service/HoldingService.java`  
函数：`addCash(CashDepositRequest request)`

处理步骤：

1. **金额校验**
   - `amount == null` 或 `amount <= 0` 时返回 `400`。

2. **解析币种**
   - 调用 `request.resolveCurrencyCode()`。
   - 取值优先级：`currencyCode` -> `currency` -> `cashCurrency` -> 默认 `USD`。

3. **查询汇率（币种 -> USD）**
   - 调用 `cashFxService.getUsdQuote(...)`，返回 `FxQuote(currencyCode, usdRate)`。

4. **确保现金资产存在**
   - 调用 `assetCatalogRepository.ensureCashAsset(fxQuote.currencyCode())`。
   - 不存在则创建 `CASH_XXX` 资产。

5. **生成本次现金持仓对象**
   - `quantity = amount`（4 位小数）
   - `purchasePrice = usdRate`（6 位小数）
   - `purchaseTime = now`

6. **持仓合并或新增**
   - 调 `portfolioItemRepository.findByAssetCatalogId(...)` 查询是否已有该币种现金持仓。
   - 有则 `updateHolding(...)`（数量累加、汇率按加权平均）。
   - 无则 `save(...)` 新增。

7. **记录交易流水**
   - `tradeHistoryRepository.save(new TradeHistory(..., "DEPOSIT", ...))`。

8. **返回最新持仓并刷新组合表现**
   - `getItem(id)` 回读最新持仓。
   - `performanceService.recordCurrentPortfolioValue()` 更新组合价值快照。

---

### 3.4 汇率服务与外部 REST API

#### 币种校验与异常映射
文件：`src/main/java/com/mina/minaportfoliomanagement/service/CashFxService.java`  
函数：`getUsdQuote(...)`, `normalizeCurrency(...)`

规则：
- 支持币种：`USD, GBP, EUR, CNY, JPY, HKD`
- 不支持/为空：`400 Bad Request`
- 外部汇率调用失败：转为 `502 Bad Gateway`

#### 外部汇率调用
文件：`src/main/java/com/mina/minaportfoliomanagement/client/FxRateApiClient.java`  
函数：`getToUsdRate(String sourceCurrency)`

调用地址：
- `https://api.frankfurter.dev/v1/latest?from=%s&to=USD`

逻辑：
- `USD` 直接返回 `1.0`
- 其他币种请求外部 API 并解析 `USD` 汇率
- 非 200 / 解析失败会抛异常（由上层映射为 502）

---

## 4. 数据库写入点

### 4.1 `asset_catalog`（必要时新增）
文件：`AssetCatalogRepository.ensureCashAsset(...)`

- `ticker = CASH_<CURRENCY>`
- `asset_name = Cash (<CURRENCY>)`
- `asset_type = CASH`

### 4.2 `portfolio_item`（新增或更新）
文件：`PortfolioItemRepository`

- 新增：`save(PortfolioItem item)`
- 更新：`updateHolding(id, quantity, averagePurchasePrice, latestPurchaseTime)`

### 4.3 `trade_history`（每次入金都记录）
文件：`TradeHistoryRepository.save(...)`

- `trade_type = DEPOSIT`
- `quantity = 入金额`
- `trade_price = 当时汇率（币种 -> USD）`
- `trade_time = 当前时间`

---

## 5. 响应与错误码

### 成功
- `201 Created`
- 返回 `HoldingView`（ticker 如 `CASH_EUR`）

### 失败
- `400 Bad Request`
  - amount 无效
  - currencyCode 缺失或不支持
- `502 Bad Gateway`
  - 外部 FX API 不可用/超时/异常

---

## 6. 时序图（文本）

```text
User
  -> Frontend(app.js saveCash)
  -> POST /api/portfolio-items/cash {amount, currencyCode}
  -> HoldingController.addCash
  -> HoldingService.addCash
      -> CashDepositRequest.resolveCurrencyCode
      -> CashFxService.getUsdQuote
          -> FxRateApiClient.getToUsdRate (Frankfurter REST)
      -> AssetCatalogRepository.ensureCashAsset
      -> PortfolioItemRepository.findByAssetCatalogId
      -> (updateHolding | save)
      -> TradeHistoryRepository.save(DEPOSIT)
      -> PerformanceService.recordCurrentPortfolioValue
      -> getItem
  <- 201 Created + HoldingView
Frontend <- toast + reload holdings
```

---

## 7. 涉及的主要 Java 文件与函数清单

- `controller/HoldingController.java`
  - `addCash(CashDepositRequest request)`

- `service/HoldingService.java`
  - `addCash(CashDepositRequest request)`
  - `getItem(long id)`

- `dto/CashDepositRequest.java`
  - `resolveCurrencyCode()`

- `service/CashFxService.java`
  - `getUsdQuote(String inputCurrencyCode)`
  - `normalizeCurrency(String inputCurrencyCode)`

- `client/FxRateApiClient.java`
  - `getToUsdRate(String sourceCurrency)`

- `repository/AssetCatalogRepository.java`
  - `ensureCashAsset(String currencyCode)`
  - `findByTicker(String ticker)`

- `repository/PortfolioItemRepository.java`
  - `findByAssetCatalogId(long assetCatalogId)`
  - `save(PortfolioItem item)`
  - `updateHolding(long id, BigDecimal quantity, BigDecimal averagePurchasePrice, LocalDateTime latestPurchaseTime)`
  - `findById(long id)`

- `repository/TradeHistoryRepository.java`
  - `save(TradeHistory trade)`

- `service/PerformanceService.java`
  - `recordCurrentPortfolioValue()`
