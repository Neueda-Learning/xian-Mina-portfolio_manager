package com.mina.minaportfoliomanagement.client;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FxRateApiClient {

    private static final Pattern USD_RATE_PATTERN = Pattern.compile("\\\"USD\\\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
    private static final String API_URL = "https://api.frankfurter.dev/v1/latest?from=%s&to=USD";

    private final HttpClient httpClient;

    public FxRateApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public BigDecimal getToUsdRate(String sourceCurrency) {
        String normalized = sourceCurrency.toUpperCase();
        if ("USD".equals(normalized)) {
            return BigDecimal.ONE;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL.formatted(normalized)))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("FX API returned HTTP " + response.statusCode());
            }
            Matcher matcher = USD_RATE_PATTERN.matcher(response.body());
            if (!matcher.find()) {
                throw new IllegalStateException("FX API did not return USD rate for " + normalized);
            }
            return new BigDecimal(matcher.group(1)).setScale(6, RoundingMode.HALF_UP);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to call FX API for " + normalized, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("FX API request was interrupted", exception);
        }
    }
}
