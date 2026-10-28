package com.rentrewards.challenge.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which webhook events have already been processed, so that the
 * RewardsEngine can ignore duplicate deliveries from the payment processor.
 */
public class ProcessedEventStore {

    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    /**
     * Returns whether this is the first delivery of the given event, recording it as delivered.
     * Not a read-only check: a second call with the same eventId returns false.
     */
    public boolean isFirstDelivery(String eventId) {
        return processedEventIds.add(eventId);
    }
}
