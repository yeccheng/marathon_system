package com.ape.marathon.api.feign;

import com.ape.marathon.api.dto.RegistrationBrief;
import com.ape.marathon.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "marathon-registration", contextId = "registrationFeignClient")
public interface RegistrationFeignClient {

    @GetMapping("/internal/registration/{id}")
    Result<RegistrationBrief> getBrief(@PathVariable("id") Long id);

    @GetMapping("/internal/registration/count")
    Result<Integer> countActiveByEvent(@RequestParam("eventId") Long eventId);

    @GetMapping("/internal/registration/exists")
    Result<Boolean> existsByEvent(@RequestParam("eventId") Long eventId);

    @GetMapping("/internal/registration/exists-by-runner")
    Result<Boolean> existsByRunner(@RequestParam("runnerId") Long runnerId);
}
