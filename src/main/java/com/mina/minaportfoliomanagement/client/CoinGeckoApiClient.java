package com.mina.minaportfoliomanagement.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mina.minaportfoliomanagement.dto.MarketQuote;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/** 调用 CoinGecko Demo API，读取 BTC、ETH 的最新美元价格。 */
@Component
public class CoinGeckoApiClient {

    private static final String PRICE_PATH = "/simple/price?ids=bitcoin,ethereum"
            + "&vs_currencies=usd&include_last_updated_at=true";
    private static final ZoneId APPLICATION_ZONE = ZoneId.of("Asia/Shanghai");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;

    public CoinGeckoApiClient(@Value("${coingecko.api-key:}") String apiKey,
                              @Value("${coingecko.base-url:https://api.coingecko.com/api/v3}") String baseUrl) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    /** 是否已从 Windows 环境变量读取到 Demo API Key。 */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** 一次请求同时获取 bitcoin 和 ethereum，避免浪费免费 API 调用额度。 */
    public Map<String, MarketQuote> getLatestPrices() {
        if (!isConfigured()) {
            throw new IllegalStateException("COINGECKO_API_KEY is not configured");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + PRICE_PATH))
                .header("x-cg-demo-api-key", apiKey)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("CoinGecko API returned HTTP " + response.statusCode());
            }
            return parsePrices(response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to call CoinGecko API", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("CoinGecko API request was interrupted", exception);
        }
    }

    /** CoinGecko 使用 coin id（bitcoin、ethereum），不是页面显示的 BTC、ETH ticker。 */
    private Map<String, MarketQuote> parsePrices(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            Map<String, MarketQuote> prices = new HashMap<>();
            addQuote(root, "bitcoin", prices);
            addQuote(root, "ethereum", prices);
            return prices;
        } catch (IOException exception) {
            throw new IllegalStateException("CoinGecko API returned invalid JSON", exception);
        }
    }

    private void addQuote(JsonNode root, String coinId, Map<String, MarketQuote> prices) {
        JsonNode coin = root.path(coinId);
        JsonNode priceNode = coin.path("usd");
        JsonNode updatedAtNode = coin.path("last_updated_at");
        if (!priceNode.isNumber() || !updatedAtNode.canConvertToLong()) {
            throw new IllegalStateException("CoinGecko API did not return a valid price for " + coinId);
        }

        BigDecimal price = priceNode.decimalValue().setScale(2, RoundingMode.HALF_UP);
        LocalDateTime priceTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(updatedAtNode.asLong()), APPLICATION_ZONE);
        prices.put(coinId, new MarketQuote(price, priceTime));
    }
}
