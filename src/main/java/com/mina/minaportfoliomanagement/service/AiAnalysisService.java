package com.mina.minaportfoliomanagement.service;

import com.mina.minaportfoliomanagement.dto.HoldingView;
import com.mina.minaportfoliomanagement.dto.MarketAssetView;

import com.mina.minaportfoliomanagement.model.PortfolioItem;
import com.mina.minaportfoliomanagement.model.PortfolioValueHistory;
import com.mina.minaportfoliomanagement.repository.AssetCatalogRepository;
import com.mina.minaportfoliomanagement.repository.PortfolioItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiAnalysisService {

    private static final Pattern CONTENT_PATTERN = Pattern.compile(
            "\\\"content\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");

    private final PortfolioItemRepository portfolioItemRepository;
    private final AssetCatalogRepository assetCatalogRepository;
    private final PortfolioService portfolioService;
    private final HttpClient httpClient;

    @Value("${deepseek.api-key:}")
    private String apiKey;
    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;
    @Value("${deepseek.model:deepseek-v4-flash}")
    private String model;

    public AiAnalysisService(PortfolioItemRepository portfolioItemRepository, AssetCatalogRepository assetCatalogRepository,
                             PortfolioService portfolioService) {
        this.portfolioItemRepository = portfolioItemRepository;
        //this.portfolioItemRepository = portfolioItemRepository;
        this.assetCatalogRepository = assetCatalogRepository;
        this.portfolioService = portfolioService;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    /** 在独立线程中请求模型，避免阻塞 Spring MVC 的请求线程。 */
    public void streamAnalysis(Long portfolioId, SseEmitter emitter) {
        if (apiKey == null || apiKey.isBlank()) {
            sendEvent(emitter, "ai-error", "未配置 DEEPSEEK_API_KEY，无法调用 AI 分析。");
            emitter.complete();
            return;
        }

        try {
            String requestBody = buildRequestBody(portfolioService.requirePortfolioId(portfolioId));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<InputStream> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                sendEvent(emitter, "ai-error", "DeepSeek 调用失败，HTTP 状态：" + response.statusCode());
                emitter.complete();
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) {
                        continue;
                    }
                    String data = line.substring(6);
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    String token = readContentToken(data);
                    if (!token.isEmpty()) {
                        sendEvent(emitter, "token", token);
                    }
                }
            }
            sendEvent(emitter, "complete", "done");
            emitter.complete();
        } catch (Exception exception) {
            sendEvent(emitter, "ai-error", "AI 分析暂时不可用：" + exception.getMessage());
            emitter.complete();
        }
    }
    /** 将本地数据放入提示词；模型只能根据这些内容分析，不能编造价格。 */
    private String buildRequestBody(long portfolioId) {
        String systemPrompt = "你是培训项目中的投资组合分析助手。只根据提供的数据分析，不提供保证收益、买卖指令或虚构数据。"
                + "必须使用中文，并严格按以下四个 Markdown 标题输出，每个标题下最多三条简短要点："
                + "## 市场概览\n## 持仓分析\n## 风险提示\n## 操作结论\n"
                + "操作结论只能给出学习用途的观察建议，必须包含‘仅供学习，不构成投资建议’。";

        List<HoldingView> holdings = portfolioItemRepository.findAll(portfolioId);
        List<MarketAssetView> marketAssets = assetCatalogRepository.findAllWithLatestPrice();
        String userData =
                "当前持仓：\n" + buildHoldingText(holdings)
                        + "\n最新市场行情：\n" + buildMarketText(marketAssets);

        return "{"
                + "\"model\":\"" + escapeJson(model) + "\","
                + "\"stream\":true,"
                + "\"thinking\":{\"type\":\"disabled\"},"
                + "\"temperature\":0.2,"
                + "\"max_tokens\":700,"
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + escapeJson(systemPrompt) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + escapeJson(userData) + "\"}]}";
    }

    private String buildHoldingText(List<HoldingView> holdings) {
        if (holdings.isEmpty()) {
            return "当前没有持仓。";
        }
        StringBuilder text = new StringBuilder();
        for (HoldingView holding : holdings) {
            text.append(holding.getTicker())
                    .append("，数量=").append(holding.getQuantity())
                    .append("，平均成本=").append(holding.getPurchasePrice())
                    .append("，当前价=").append(holding.getCurrentPrice())
                    .append('\n');
        }
        return text.toString();
    }

    private String buildMarketText(List<MarketAssetView> marketAssets) {
        StringBuilder text = new StringBuilder();
        for (MarketAssetView asset : marketAssets) {
            text.append(asset.getTicker())
                    .append("，最新价=").append(asset.getMarketPrice())
                    .append("，时间=").append(asset.getPriceTime())
                    .append('\n');
        }
        return text.toString();
    }

    /** 从 DeepSeek 的 SSE JSON 块中取得 choices[0].delta.content。 */
    private String readContentToken(String data) {
        Matcher matcher = CONTENT_PATTERN.matcher(data);
        return matcher.find() ? unescapeJson(matcher.group(1)) : "";
    }

    private void sendEvent(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (Exception ignored) {
            // 浏览器关闭连接后无需继续发送。
        }
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private String unescapeJson(String value) {
        return value.replace("\\n", "\n").replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }


}
