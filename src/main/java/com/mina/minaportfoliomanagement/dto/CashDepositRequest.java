package com.mina.minaportfoliomanagement.dto;

import java.math.BigDecimal;

public class CashDepositRequest {
    private BigDecimal amount;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
