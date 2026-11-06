package com.ape.marathon.ai.service;

import com.ape.marathon.ai.dto.EventAssistantReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class EventAssistantService {

    private static final Logger log = LoggerFactory.getLogger(EventAssistantService.class);

    private final ChatClient chatClient;

    public EventAssistantService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Tool Calling 查赛事 + entity 结构化输出
     */
    public EventAssistantReply ask(String question) {
        log.info("assistant ask: {}", question);
        EventAssistantReply reply = chatClient.prompt()
                .user(question)
                .call()
                .entity(EventAssistantReply.class);

        if (reply == null) {
            reply = new EventAssistantReply();
            reply.setAnswer("模型未返回有效结构，请稍后重试。");
            reply.setGrounded(false);
            return reply;
        }
        if (reply.getAnswer() == null || reply.getAnswer().isBlank()) {
            reply.setAnswer("暂无回答内容。");
        }
        if (reply.getEvents() != null && !reply.getEvents().isEmpty()) {
            reply.setGrounded(true);
        }
        return reply;
    }
}
