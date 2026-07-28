package com.mina.minaportfoliomanagement.dto;

import java.math.BigDecimal;

public class SellRequest {
    private BigDecimal quantity;

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
