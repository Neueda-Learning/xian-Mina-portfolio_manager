package com.mina.minaportfoliomanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 外部价格 API 解析后的一条真实报价：价格和对应的五分钟时间点。 */
public record MarketQuote(BigDecimal price, LocalDateTime priceTime) {
}
