package com.ape.marathon.ai.controller;

import com.ape.marathon.ai.common.ApiResult;
import com.ape.marathon.ai.dto.ChatRequest;
import com.ape.marathon.ai.dto.EventAssistantReply;
import com.ape.marathon.ai.service.EventAssistantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AssistantController {

    private final EventAssistantService eventAssistantService;

    public AssistantController(EventAssistantService eventAssistantService) {
        this.eventAssistantService = eventAssistantService;
    }

    @GetMapping("/health")
    public ApiResult<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "marathon-ai-service");
        data.put("status", "UP");
        ApiResult<Map<String, Object>> result = new ApiResult<>();
        result.setCode(200);
        result.setMsg("ok");
        result.setData(data);
        return result;
    }

    /**
     * POST /api/ai/chat
     * body: {"question":"现在有哪些赛事还能报名？"}
     */
    @PostMapping("/chat")
    public ApiResult<EventAssistantReply> chat(@Valid @RequestBody ChatRequest request) {
        EventAssistantReply reply = eventAssistantService.ask(request.getQuestion());
        ApiResult<EventAssistantReply> result = new ApiResult<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(reply);
        return result;
    }
}
