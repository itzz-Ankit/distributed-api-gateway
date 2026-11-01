package com.platform.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/user")
    public Mono<ResponseEntity<Map<String, Object>>> userFallback() {
        return Mono.just(createResponse("User Service"));
    }

    @RequestMapping("/fallback/order")
    public Mono<ResponseEntity<Map<String, Object>>> orderFallback() {
        return Mono.just(createResponse("Order Service"));
    }

    private ResponseEntity<Map<String, Object>> createResponse(String service) {
        Map<String, Object> response = new HashMap<>();

        response.put("success", false);
        response.put("service", service);
        response.put("message",
                service + " is currently unavailable. Please try again later.");

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }
}