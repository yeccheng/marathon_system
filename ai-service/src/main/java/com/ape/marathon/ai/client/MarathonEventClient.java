package com.ape.marathon.ai.client;

import com.ape.marathon.ai.common.ApiResult;
import com.ape.marathon.ai.config.MarathonClientProperties;
import com.ape.marathon.ai.dto.EventCard;
import com.ape.marathon.ai.dto.EventDetailView;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

/**
 * 调用主工程公开赛事 API（不改动 Boot 2.7 主服务）。
 */
@Component
public class MarathonEventClient {

    private final RestTemplate restTemplate;
    private final MarathonClientProperties properties;

    public MarathonEventClient(RestTemplate restTemplate, MarathonClientProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public List<EventCard> listOpenEvents() {
        return fetchEventList("/api/public/events");
    }

    public List<EventCard> listAllEvents() {
        return fetchEventList("/api/public/events/all");
    }

    public EventDetailView getEventDetail(Long id) {
        String url = UriComponentsBuilder
                .fromUriString(properties.getBaseUrl())
                .path("/api/public/event/{id}")
                .buildAndExpand(id)
                .toUriString();
        try {
            ResponseEntity<ApiResult<EventDetailView>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResult<EventDetailView>>() {});
            ApiResult<EventDetailView> body = response.getBody();
            if (body == null || !body.ok()) {
                return null;
            }
            return body.getData();
        } catch (RestClientException ex) {
            throw new IllegalStateException("调用主工程赛事详情失败: " + ex.getMessage(), ex);
        }
    }

    private List<EventCard> fetchEventList(String path) {
        String url = UriComponentsBuilder
                .fromUriString(properties.getBaseUrl())
                .path(path)
                .toUriString();
        try {
            ResponseEntity<ApiResult<List<EventCard>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResult<List<EventCard>>>() {});
            ApiResult<List<EventCard>> body = response.getBody();
            if (body == null || !body.ok() || body.getData() == null) {
                return Collections.emptyList();
            }
            return body.getData();
        } catch (RestClientException ex) {
            throw new IllegalStateException("调用主工程赛事列表失败: " + ex.getMessage(), ex);
        }
    }
}
