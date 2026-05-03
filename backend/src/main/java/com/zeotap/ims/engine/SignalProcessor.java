package com.zeotap.ims.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import com.zeotap.ims.model.Signal;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignalProcessor {

    private final SignalBuffer signalBuffer;
    private final DebounceEngine debounceEngine;

    // Drain and process buffer every 100ms
    @Async
    @Scheduled(fixedDelay = 100)
    public void processBuffer() {
        List<Signal> batch = signalBuffer.drainBatch(500);
        if (!batch.isEmpty()) {
            debounceEngine.processBatch(batch);
        }
    }

    // Print throughput metrics every 5 seconds - required by assignment
    @Scheduled(fixedDelay = 5000)
    public void printMetrics() {
        long signalsInWindow = signalBuffer.getAndResetWindowCount();
        double signalsPerSecond = signalsInWindow / 5.0;

        log.info("======= IMS THROUGHPUT METRICS =======");
        log.info("Signals/sec     : {}", String.format("%.1f", signalsPerSecond));
        log.info("Buffer size     : {}/{}", 
            signalBuffer.getCurrentSize(),
            signalBuffer.getCapacity());
        log.info("Total received  : {}", signalBuffer.getTotalReceived());
        log.info("======================================");
    }
}
