package com.rentrewards.challenge.service;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessedEventStoreTest {

    @Test
    void isFirstDeliveryReturnsTrueForANewEvent() {
        ProcessedEventStore store = new ProcessedEventStore();

        assertTrue(store.isFirstDelivery("evt-1"));
    }

    @Test
    void isFirstDeliveryReturnsFalseOnSecondCall() {
        ProcessedEventStore store = new ProcessedEventStore();

        store.isFirstDelivery("evt-1");

        assertFalse(store.isFirstDelivery("evt-1"));
    }

    @Test
    void remembersOlderEventsAfterNewerOnesArrive() {
        ProcessedEventStore store = new ProcessedEventStore();

        assertTrue(store.isFirstDelivery("evt-A"));
        assertTrue(store.isFirstDelivery("evt-B"));

        assertFalse(store.isFirstDelivery("evt-A"));
    }

    @RepeatedTest(8)
    void onlyOneConcurrentCallerSeesTheFirstDelivery() throws Exception {
        int workerCount = 16;
        ProcessedEventStore store = new ProcessedEventStore();
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        CountDownLatch ready = new CountDownLatch(workerCount);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int worker = 0; worker < workerCount; worker++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return store.isFirstDelivery("evt-concurrent");
                }));
            }

            assertTrue(ready.await(2, TimeUnit.SECONDS));
            start.countDown();

            long firstDeliveries = 0;
            for (Future<Boolean> future : futures) {
                if (future.get(2, TimeUnit.SECONDS)) {
                    firstDeliveries++;
                }
            }

            assertEquals(1, firstDeliveries);
        } finally {
            executor.shutdownNow();
        }
    }
}
