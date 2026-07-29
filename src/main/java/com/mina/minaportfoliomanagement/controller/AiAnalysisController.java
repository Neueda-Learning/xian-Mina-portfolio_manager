package com.mina.minaportfoliomanagement.controller;

import com.mina.minaportfoliomanagement.service.AiAnalysisService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

/** 对前端提供 DeepSeek 分析的 SSE 流式接口。 */
@RestController
@RequestMapping("/api/ai-analysis")
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    public AiAnalysisController(AiAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAnalysis() {
        SseEmitter emitter = new SseEmitter(120_000L);
        CompletableFuture.runAsync(() -> aiAnalysisService.streamAnalysis(emitter));
        return emitter;
    }
}
