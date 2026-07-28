package com.mina.minaportfoliomanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 前端选择买入时间时使用的一条数据库历史价格。 */
public record PriceHistoryView(BigDecimal marketPrice, LocalDateTime priceTime) {
}
