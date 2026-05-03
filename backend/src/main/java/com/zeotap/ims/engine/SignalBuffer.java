package com.zeotap.ims.engine;

import com.zeotap.ims.model.Signal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class SignalBuffer {

    // Buffer capacity - holds up to 10,000 signals in memory
    private static final int BUFFER_CAPACITY = 10_000;

    // The actual in-memory buffer
    private final BlockingQueue<Signal> buffer =
        new ArrayBlockingQueue<>(BUFFER_CAPACITY);

    // Tracks total signals received - used for metrics
    private final AtomicLong totalReceived = new AtomicLong(0);

    // Tracks signals received in current 5s window - for throughput metrics
    private final AtomicLong currentWindowCount = new AtomicLong(0);

    // Try to add signal to buffer - non blocking
    // Returns false if buffer is full (backpressure)
    public boolean offer(Signal signal) {
        boolean accepted = buffer.offer(signal);
        if (accepted) {
            totalReceived.incrementAndGet();
            currentWindowCount.incrementAndGet();
        } else {
            log.warn("Buffer full! Dropping signal from component: {}",
                signal.getComponentId());
        }
        return accepted;
    }

    // Worker thread drains signals from buffer in batches
    public List<Signal> drainBatch(int maxBatchSize) {
        List<Signal> batch = new ArrayList<>(maxBatchSize);
        buffer.drainTo(batch, maxBatchSize);
        return batch;
    }

    public int getCurrentSize() {
        return buffer.size();
    }

    public int getCapacity() {
        return BUFFER_CAPACITY;
    }

    public long getTotalReceived() {
        return totalReceived.get();
    }

    // Returns count and resets window counter - called every 5 seconds
    public long getAndResetWindowCount() {
        return currentWindowCount.getAndSet(0);
    }
}
