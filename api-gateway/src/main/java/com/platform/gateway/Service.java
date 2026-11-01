package com.platform.gateway;

import Dto.request;
import Dto.response;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@org.springframework.stereotype.Service
public class Service {

    private final MeterRegistry meterRegistry;

    @Autowired
    public Service(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    private final ConcurrentHashMap<String, LongAdder> requestCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> successfulCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> failedCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> totalLatencyMs = new ConcurrentHashMap<>();

    public void recordRequest(String serviceName) {
        recordRequest(serviceName, 200, 0L);
    }

    public void recordRequest(String serviceName, int statusCode) {
        recordRequest(serviceName, statusCode, 0L);
    }

    public void recordRequest(String serviceName, int statusCode, long latencyMs) {
        String name = serviceName == null || serviceName.trim().isEmpty() ? "unknown" : serviceName;
        requestCount.computeIfAbsent(name, key -> new LongAdder()).increment();
        if (statusCode >= 200 && statusCode < 400) {
            successfulCount.computeIfAbsent(name, key -> new LongAdder()).increment();
        } else {
            failedCount.computeIfAbsent(name, key -> new LongAdder()).increment();
        }
        totalLatencyMs.computeIfAbsent(name, key -> new LongAdder()).add(Math.max(0L, latencyMs));

        meterRegistry.counter("gateway.observability.requests", "service", name).increment();
        meterRegistry.counter("gateway.observability.requests.completed", "service", name,
                "outcome", statusCode >= 200 && statusCode < 400 ? "success" : "failure").increment();
        meterRegistry.timer("gateway.observability.request.duration", "service", name)
                .record(Math.max(0L, latencyMs), java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public response getObservability(request request) {
        response result = new response();
        String serviceName = request == null || request.getServiceName() == null
                || request.getServiceName().trim().isEmpty() ? "unknown" : request.getServiceName();
        long total = getCount(requestCount, serviceName);
        long successful = getCount(successfulCount, serviceName);
        long failed = getCount(failedCount, serviceName);
        long totalLatency = getCount(totalLatencyMs, serviceName);
        result.setServiceName(serviceName);
        result.setTotalRequests(total);
        result.setSuccessfulRequests(successful);
        result.setFailedRequests(failed);
        result.setErrorRate(total == 0 ? 0.0 : (double) failed / total);
        result.setAverageLatencyMs(total == 0 ? 0.0 : (double) totalLatency / total);
        return result;
    }

    private long getCount(ConcurrentHashMap<String, LongAdder> counts, String serviceName) {
        LongAdder count = counts.get(serviceName);
        return count == null ? 0L : count.sum();
    }
}
