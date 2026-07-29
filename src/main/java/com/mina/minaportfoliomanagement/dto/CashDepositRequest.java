package com.mina.minaportfoliomanagement.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;

public class CashDepositRequest {
    private static final String DEFAULT_CURRENCY_CODE = "USD";
    private BigDecimal amount;
    private String currencyCode;
    private String currency;
    private String cashCurrency;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    @JsonAlias("currency")
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @JsonAlias("cashCurrency")
    public void setCashCurrency(String cashCurrency) {
        this.cashCurrency = cashCurrency;
    }

    public String resolveCurrencyCode() {
        if (currencyCode != null && !currencyCode.isBlank()) {
            return currencyCode;
        }
        if (currency != null && !currency.isBlank()) {
            return currency;
        }
        if (cashCurrency != null && !cashCurrency.isBlank()) {
            return cashCurrency;
        }
        return DEFAULT_CURRENCY_CODE;
    }
}
