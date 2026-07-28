package com.mina.minaportfoliomanagement.client;

import com.mina.minaportfoliomanagement.dto.MarketQuote;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 调用培训提供的缓存金融价格 API，并解析完整的五分钟 close / timestamp 数据。 */
@Component
public class MarketPriceApiClient {

    private static final String API_URL =
            "https://c4rm9elh30.execute-api.us-east-1.amazonaws.com/default/cachedPriceData?ticker=";
    private static final Pattern CLOSE_ARRAY_PATTERN = Pattern.compile("\\\"close\\\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);
    private static final Pattern TIMESTAMP_ARRAY_PATTERN = Pattern.compile("\\\"timestamp\\\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);
    private static final DateTimeFormatter API_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final HttpClient httpClient;

    public MarketPriceApiClient() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    /** 查询指定 ticker 的全部有效历史报价。 */
    public List<MarketQuote> getPriceHistory(String ticker) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + ticker))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Price API returned HTTP " + response.statusCode());
            }
            return parsePriceHistory(response.body(), ticker);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to call Price API for " + ticker, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Price API request was interrupted", exception);
        }
    }

    /** close 和 timestamp 数组按下标一一对应；null 或坏数据跳过。 */
    private List<MarketQuote> parsePriceHistory(String responseBody, String ticker) {
        Matcher closeMatcher = CLOSE_ARRAY_PATTERN.matcher(responseBody);
        Matcher timeMatcher = TIMESTAMP_ARRAY_PATTERN.matcher(responseBody);
        if (!closeMatcher.find() || !timeMatcher.find()) {
            throw new IllegalStateException("Price API did not return complete quote data for " + ticker);
        }

        String[] prices = closeMatcher.group(1).split(",");
        String[] times = timeMatcher.group(1).replace("\"", "").split(",");
        List<MarketQuote> quotes = new ArrayList<>();

        for (int index = 0; index < Math.min(prices.length, times.length); index++) {
            String value = prices[index].trim();
            if ("null".equalsIgnoreCase(value) || value.isEmpty()) {
                continue;
            }
            try {
                BigDecimal price = new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
                LocalDateTime priceTime = LocalDateTime.parse(times[index].trim(), API_TIME_FORMAT);
                quotes.add(new MarketQuote(price, priceTime));
            } catch (NumberFormatException | DateTimeParseException ignored) {
                // 单个元素异常不影响同一 API 响应中的其他有效价格。
            }
        }
        if (quotes.isEmpty()) {
            throw new IllegalStateException("Price API did not return a valid close price for " + ticker);
        }
        return quotes;
    }
}
