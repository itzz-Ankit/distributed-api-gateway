package com.platform.gateway.observability;

import org.springframework.stereotype.Component;

import java.util.BitSet;

/**
 * Small in-memory Bloom filter for fast duplicate checks.
 * Elasticsearch remains the source of truth when this filter reports a hit.
 */
@Component
public class GatewayEventBloomFilter {

    private static final int SIZE = 1_000_003;
    private static final int[] SEEDS = {31, 37, 41, 43, 47};

    private final BitSet bits = new BitSet(SIZE);

    public synchronized void add(String value) {
        if (value == null) {
            return;
        }

        for (int seed : SEEDS) {
            bits.set(index(value, seed));
        }
    }

    public synchronized boolean mightContain(String value) {
        if (value == null) {
            return false;
        }

        for (int seed : SEEDS) {
            if (!bits.get(index(value, seed))) {
                return false;
            }
        }
        return true;
    }

    private int index(String value, int seed) {
        int hash = value.hashCode() * seed;
        hash ^= hash >>> 16;
        return (hash & Integer.MAX_VALUE) % SIZE;
    }
}
