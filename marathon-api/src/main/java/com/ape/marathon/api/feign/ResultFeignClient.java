package com.ape.marathon.api.feign;

import com.ape.marathon.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "marathon-result", contextId = "resultFeignClient")
public interface ResultFeignClient {

    @GetMapping("/internal/result/finishers")
    Result<Integer> countFinishers(@RequestParam("eventId") Long eventId);
}
