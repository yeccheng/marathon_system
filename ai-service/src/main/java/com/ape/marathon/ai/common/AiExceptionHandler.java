package com.ape.marathon.ai.common;

import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AiExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> badRequest(Exception ex) {
        ApiResult<Void> result = new ApiResult<>();
        result.setCode(400);
        result.setMsg(ex.getMessage());
        return result;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> serverError(Exception ex) {
        ApiResult<Void> result = new ApiResult<>();
        result.setCode(500);
        result.setMsg(friendlyMessage(ex));
        return result;
    }

    private static String friendlyMessage(Exception ex) {
        String raw = ex.getMessage() == null ? "" : ex.getMessage();
        String lower = raw.toLowerCase();
        if (lower.contains("invalid_api_key") || lower.contains("incorrect api key")) {
            return "大模型 API Key 无效。请检查 ai-service/application-local.yml 或环境变量 DEEPSEEK_API_KEY，并确认 Key 所属平台与 base-url 一致（当前默认 DeepSeek）。";
        }
        if (lower.contains("401") || lower.contains("unauthorized")) {
            return "大模型鉴权失败（401）。请核对 API Key 与服务商地址。";
        }
        if (raw.isBlank()) {
            return "AI 服务异常";
        }
        // 避免把整段 JSON 堆给前端
        int cut = raw.indexOf(" - {");
        return cut > 0 ? raw.substring(0, cut) : raw;
    }
}
