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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 调用 Twelve Data 获取基金的最新日净值。
 * 基金不是实时价格，接口通常返回上一个交易日公布的 NAV。
 */
@Component
public class TwelveDataApiClient {

    private static final ZoneId APPLICATION_ZONE = ZoneId.of("Asia/Shanghai");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;

    public TwelveDataApiClient(@Value("${twelvedata.api-key:}") String apiKey,
                               @Value("${twelvedata.base-url:https://api.twelvedata.com}") String baseUrl) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    /** Windows 环境变量中有 Key 时，才允许请求基金接口。 */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 查询单只基金最近 30 个交易日的日净值。
     * 基金没有分钟级报价，因此 values 中的每一行代表一个交易日的收盘净值。
     */
    public List<MarketQuote> getFundPriceHistory(String ticker) {
        if (!isConfigured()) {
            throw new IllegalStateException("TWELVE_DATA_API_KEY is not configured");
        }

        String path = "/time_series?symbol=" + ticker
                + "&type=Mutual%20Fund&interval=1day&outputsize=30";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "apikey " + apiKey)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Twelve Data API returned HTTP " + response.statusCode());
            }
            return parsePriceHistory(response.body(), ticker);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to call Twelve Data API", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Twelve Data API request was interrupted", exception);
        }
    }

    /** 将 Twelve Data 的 values 数组转换为项目统一的历史报价列表。 */
    private List<MarketQuote> parsePriceHistory(String responseBody, String ticker) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String status = root.path("status").asText();
            // Twelve Data 成功响应也会带 status: "ok"，不能仅因有 status 字段就判定失败。
            if (root.has("code") || (!status.isBlank() && !"ok".equalsIgnoreCase(status))) {
                String message = root.path("message").asText("unknown API error");
                throw new IllegalStateException("Twelve Data API did not return valid price history for "
                        + ticker + ": " + message);
            }

            JsonNode values = root.path("values");
            if (!values.isArray() || values.isEmpty()) {
                throw new IllegalStateException("Twelve Data API did not return price history for " + ticker);
            }

            List<MarketQuote> quotes = new ArrayList<>();
            for (JsonNode value : values) {
                quotes.add(toMarketQuote(value, ticker));
            }
            return quotes;
        } catch (IOException exception) {
            throw new IllegalStateException("Twelve Data API returned invalid JSON", exception);
        }
    }

    /** 读取某个交易日的 close（基金净值）和日期。 */
    private MarketQuote toMarketQuote(JsonNode value, String ticker) {
        JsonNode closeNode = value.path("close");
        if (closeNode.isMissingNode() || closeNode.asText().isBlank()) {
            throw new IllegalStateException("Twelve Data API returned an invalid close price for " + ticker);
        }

        try {
            BigDecimal price = new BigDecimal(closeNode.asText()).setScale(2, RoundingMode.HALF_UP);
            return new MarketQuote(price, readPriceTime(value));
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Twelve Data API returned an invalid close price for " + ticker, exception);
        }
    }

    /** 优先使用 API 的 Unix 时间戳；没有时间戳时再使用 datetime 日期。 */
    private LocalDateTime readPriceTime(JsonNode root) {
        JsonNode timestampNode = root.path("timestamp");
        if (timestampNode.canConvertToLong()) {
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestampNode.asLong()), APPLICATION_ZONE);
        }

        String datetime = root.path("datetime").asText();
        try {
            return LocalDateTime.parse(datetime);
        } catch (DateTimeParseException ignored) {
            try {
                // 日净值只带日期；使用当天末尾，使它会成为该日最新可交易价格。
                return LocalDate.parse(datetime).atTime(23, 59, 59);
            } catch (DateTimeParseException exception) {
                throw new IllegalStateException("Twelve Data API did not return a valid quote time", exception);
            }
        }
    }
}
