package com.platform.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
@Order(-2)
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayExceptionHandler.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    private final ObjectMapper objectMapper;

    public GatewayExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable exception) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(exception);
        }

        HttpStatus status = resolveStatus(exception);
        String path = exchange.getRequest().getURI().getPath();
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        String message = resolveMessage(status);

        log.error("Gateway error | path={} | status={} | correlationId={}",
                path, status.value(), correlationId, exception);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        if (correlationId != null) {
            body.put("correlationId", correlationId);
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException serializationException) {
            log.error("Could not serialize Gateway error response", serializationException);
            return Mono.error(serializationException);
        }
    }

    private HttpStatus resolveStatus(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ResponseStatusException) {
                return ((ResponseStatusException) current).getStatus();
            }
            if (current instanceof BadCredentialsException
                    || current instanceof AuthenticationCredentialsNotFoundException) {
                return HttpStatus.UNAUTHORIZED;
            }
            if (current instanceof AccessDeniedException) {
                return HttpStatus.FORBIDDEN;
            }
            if (current instanceof TimeoutException || current instanceof SocketTimeoutException
                    || current.getClass().getSimpleName().toLowerCase().contains("timeout")) {
                return HttpStatus.GATEWAY_TIMEOUT;
            }
            if (current instanceof ConnectException || current instanceof UnknownHostException
                    || current.getClass().getSimpleName().toLowerCase().contains("connectexception")) {
                return HttpStatus.SERVICE_UNAVAILABLE;
            }
            if (current instanceof IllegalArgumentException) {
                return HttpStatus.BAD_REQUEST;
            }
            current = current.getCause();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveMessage(HttpStatus status) {
        switch (status) {
            case BAD_REQUEST:
                return "Invalid request. Please check your input.";
            case UNAUTHORIZED:
                return "Authentication is required or the supplied token is invalid.";
            case FORBIDDEN:
                return "You do not have permission to access this resource.";
            case NOT_FOUND:
                return "The requested resource was not found.";
            case SERVICE_UNAVAILABLE:
                return "The requested downstream service is currently unavailable.";
            case GATEWAY_TIMEOUT:
                return "The downstream service did not respond in time.";
            case TOO_MANY_REQUESTS:
                return "Request limit exceeded. Please retry later.";
            default:
                return "An unexpected error occurred in the API Gateway.";
        }
    }
}
