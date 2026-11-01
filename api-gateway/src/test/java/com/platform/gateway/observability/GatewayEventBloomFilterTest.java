package com.platform.gateway.observability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayEventBloomFilterTest {

    @Test
    void addedEventCanBeFound() {
        GatewayEventBloomFilter filter = new GatewayEventBloomFilter();
        filter.add("event-123");
        assertTrue(filter.mightContain("event-123"));
    }

    @Test
    void emptyFilterDoesNotContainKnownValue() {
        GatewayEventBloomFilter filter = new GatewayEventBloomFilter();
        assertFalse(filter.mightContain("event-not-added"));
    }
}
