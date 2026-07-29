# 职责 README（个人负责模块）

本文档说明我在项目中的负责范围、接口契约与维护边界。

> 详细类图与流程图见：`ownership-diagrams.html`

## 负责模块

1. `src/main/java/com/mina/minaportfoliomanagement/controller/AiAnalysisController.java`
2. `src/main/java/com/mina/minaportfoliomanagement/service/AiAnalysisService.java`
3. `src/main/java/com/mina/minaportfoliomanagement/dto/CashDepositRequest.java`
4. `src/main/java/com/mina/minaportfoliomanagement/service/CashFxService.java`

## 职责范围

### 1) AI 分析接口层（AiAnalysisController）
- 提供 AI 分析流式接口：`GET /api/ai-analysis/stream`
- 使用 `SseEmitter` 返回 `text/event-stream`
- 异步触发服务层分析，避免阻塞请求线程

### 2) AI 分析服务层（AiAnalysisService）
- 组装分析上下文数据：
  - 投资组合持仓（`PortfolioItemRepository`）
  - 最新市场行情（`AssetCatalogRepository`）
- 构建 DeepSeek 请求（system prompt + user data）
- 调用模型流式接口并解析 token
- 通过 SSE 向前端发送事件：
  - `token`：增量内容
  - `complete`：结束标记
  - `ai-error`：错误信息
- 保障分析输出边界：仅基于提供数据、中文输出、含风险提示与学习用途声明

### 3) 现金入账请求模型（CashDepositRequest）
- 维护入参字段：`portfolioId`、`amount`、`currencyCode`
- 兼容前端历史字段：`currency`、`cashCurrency`（`@JsonAlias`）
- 提供统一币种解析逻辑：`resolveCurrencyCode()`
- 缺省币种兜底为 `USD`

### 4) 现金汇率服务（CashFxService）
- 负责现金入账币种标准化与校验（大写、空值校验、支持列表限制）
- 支持币种：`USD`、`GBP`、`EUR`、`CNY`、`JPY`、`HKD`
- 调用 `FxRateApiClient` 获取兑 USD 汇率
- 将汇率调用异常转换为业务可识别错误：
  - 参数问题 -> `400 BAD_REQUEST`
  - 外部汇率服务问题 -> `502 BAD_GATEWAY`

## 关键接口契约

### AI 分析
- **Endpoint**: `GET /api/ai-analysis/stream?portfolioId={id}`
- **Response Type**: `text/event-stream`
- **事件类型**: `token` / `complete` / `ai-error`
- **SSE 超时**: `SseEmitter(120_000ms)`
- **线程模型**: `CompletableFuture.runAsync(...)` 异步执行
- **模型调用参数（当前实现）**:
  - `stream=true`
  - `temperature=0.2`
  - `max_tokens=700`
  - `thinking.type=disabled`
  - HTTP 请求超时：`90s`，连接超时：`10s`
- **配置项**:
  - `deepseek.api-key`
  - `deepseek.base-url`（默认 `https://api.deepseek.com`）
  - `deepseek.model`（默认 `deepseek-v4-flash`）

### 现金入账相关请求体（被其他业务入口复用）
```json
{
  "portfolioId": 1,
  "amount": 100.50,
  "currencyCode": "EUR"
}
```

兼容以下别名字段：`currency`、`cashCurrency`。

## 错误语义（对外可见）

### AI 分析链路
- 未配置 API Key：通过 SSE 发送 `ai-error`，随后 `complete`
- 模型返回非 200：通过 SSE 发送 `ai-error`（含 HTTP 状态），随后 `complete`
- 运行时异常：通过 SSE 发送 `ai-error`（含异常信息），随后 `complete`

### Cash + FX 链路
- 币种为空：`400 BAD_REQUEST`，`currencyCode is required`
- 币种不支持：`400 BAD_REQUEST`，`Unsupported currencyCode`
- 汇率服务调用异常：`502 BAD_GATEWAY`，`Unable to fetch exchange rate`

## 字段兼容与默认策略

- `resolveCurrencyCode()` 优先级：
  1. `currencyCode`
  2. `currency`
  3. `cashCurrency`
  4. 默认 `USD`
- `CashFxService.normalizeCurrency()` 会执行 `trim + uppercase`，再做支持列表校验。

## 维护边界与协作点

- 我负责上述模块的接口稳定性、异常语义一致性、向后兼容与可维护性。
- 与我强依赖的上下游：
  - `PortfolioService`（组合 ID 解析）
  - `PortfolioItemRepository` / `AssetCatalogRepository`（分析数据来源）
  - `FxRateApiClient`（外部汇率能力）
- 若变更 AI 供应商、SSE 协议、币种策略或入参结构，需同步评审并更新本职责文档。
