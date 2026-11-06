package com.ape.marathon.api.feign;

import com.ape.marathon.api.dto.EventBrief;
import com.ape.marathon.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "marathon-event", contextId = "eventFeignClient")
public interface EventFeignClient {

    @GetMapping("/internal/event/{id}")
    Result<EventBrief> getBrief(@PathVariable("id") Long id);

    @PostMapping("/internal/event/{id}/quota/increase")
    Result<Boolean> increaseQuota(@PathVariable("id") Long id);

    @PostMapping("/internal/event/{id}/quota/decrease")
    Result<Boolean> decreaseQuota(@PathVariable("id") Long id);

    @PostMapping("/internal/event/{id}/quota/sync")
    Result<Boolean> syncQuota(@PathVariable("id") Long id,
                              @RequestParam("activeCount") int activeCount);
}
