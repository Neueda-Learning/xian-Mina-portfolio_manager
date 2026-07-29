# AI Analysis Service

## 1. 功能介绍

`AiAnalysisService` 是投资组合管理系统中的 AI 分析服务模块。

该服务负责：

- 获取用户当前投资组合持仓数据
- 获取最新市场行情数据
- 调用 DeepSeek 大模型进行投资组合分析
- 使用 SSE（Server-Sent Events）实现 AI 分析结果实时流式返回
- 将 AI 生成内容逐 Token 推送给前端

整体流程：
1. 用户在前端发起 AI 分析请求
2. 后端接收请求，获取用户投资组合持仓数据和最新市场行情
3. 后端调用 DeepSeek 大模型进行分析
4. DeepSeek 大模型返回分析结果，后端将结果逐 Token 推送给前端
5. 前端接收 SSE 流式数据，实时展示分析结果

系统Prompt：
你是培训项目中的投资组合分析助手。

要求：

1. 只根据提供的数据分析
2. 不虚构价格
3. 不提供保证收益
4. 不提供买卖指令
5. 必须中文输出

## 市场概览

- xxx
- xxx


## 持仓分析

- xxx
- xxx


## 风险提示

- xxx
- xxx


## 操作结论

- xxx

仅供学习，不构成投资建议

# 2. 配置参数

在application.properties 中配置：

```properties
deepseek.api-key=your_api_key

deepseek.base-url=https://api.deepseek.com

deepseek.model=deepseek-v4-flash
```
