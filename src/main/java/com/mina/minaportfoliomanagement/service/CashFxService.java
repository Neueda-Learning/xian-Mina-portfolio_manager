package com.mina.minaportfoliomanagement.service;

import com.mina.minaportfoliomanagement.client.FxRateApiClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Set;

@Service
public class CashFxService {

    public record FxQuote(String currencyCode, BigDecimal usdRate) {}

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "GBP", "EUR", "CNY", "JPY", "HKD");
    private final FxRateApiClient fxRateApiClient;

    public CashFxService(FxRateApiClient fxRateApiClient) {
        this.fxRateApiClient = fxRateApiClient;
    }

    public FxQuote getUsdQuote(String inputCurrencyCode) {
        String currencyCode = normalizeCurrency(inputCurrencyCode);
        try {
            return new FxQuote(currencyCode, fxRateApiClient.getToUsdRate(currencyCode));
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Unable to fetch exchange rate for " + currencyCode, exception);
        }
    }

    private String normalizeCurrency(String inputCurrencyCode) {
        if (inputCurrencyCode == null || inputCurrencyCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "currencyCode is required");
        }
        String normalized = inputCurrencyCode.trim().toUpperCase();
        if (!SUPPORTED_CURRENCIES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported currencyCode: " + normalized + ". Supported: " + SUPPORTED_CURRENCIES);
        }
        return normalized;
    }
}
