package com.zeotap.ims.engine;

import com.zeotap.ims.model.Signal;
import com.zeotap.ims.model.WorkItem;
import com.zeotap.ims.repository.WorkItemRepository;
import com.zeotap.ims.repository.SignalRepository;
import com.zeotap.ims.strategy.AlertStrategy;
import com.zeotap.ims.strategy.AlertStrategyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebounceEngine {

    // Debounce window - 10 seconds
    private static final long DEBOUNCE_WINDOW_MS = 10_000;

    // Tracks active debounce windows per componentId
    // Key: componentId, Value: DebounceWindow
    private final Map<String, DebounceWindow> activeWindows =
        new ConcurrentHashMap<>();

    private final WorkItemRepository workItemRepository;
    private final SignalRepository signalRepository;
    private final AlertStrategyResolver strategyResolver;

    // Process a batch of signals from the buffer
    public void processBatch(List<Signal> signals) {
        for (Signal signal : signals) {
            processSignal(signal);
        }
    }

    private void processSignal(Signal signal) {
        String componentId = signal.getComponentId();
        long now = Instant.now().toEpochMilli();

        activeWindows.compute(componentId, (id, window) -> {
            if (window == null || window.isExpired(now)) {
                // No active window - create new Work Item
                WorkItem workItem = createWorkItem(signal);
                log.info("New incident created for component: {} | Priority: {} | ID: {}",
                    componentId, workItem.getPriority(), workItem.getId());
                return new DebounceWindow(workItem.getId(), now);
            } else {
                // Active window exists - link signal to existing Work Item
                window.incrementCount();
                signal.setWorkItemId(window.getWorkItemId());
                saveSignal(signal);
                updateSignalCount(window.getWorkItemId(), window.getCount());
                log.debug("Signal linked to existing incident: {} | Total signals: {}",
                    window.getWorkItemId(), window.getCount());
                return window;
            }
        });
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    private WorkItem createWorkItem(Signal signal) {
        // Resolve alert strategy based on component type
        AlertStrategy strategy = strategyResolver.resolve(signal.getComponentType());

        // Build the work item
        WorkItem workItem = WorkItem.builder()
            .componentId(signal.getComponentId())
            .componentType(signal.getComponentType())
            .priority(strategy.getPriority())
            .state(WorkItem.IncidentState.OPEN)
            .title(strategy.generateTitle(signal))
            .startTime(signal.getTimestamp())
            .signalCount(1)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        WorkItem saved = workItemRepository.save(workItem);

        // Link the first signal to this work item
        signal.setWorkItemId(saved.getId());
        saveSignal(signal);

        return saved;
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    private void saveSignal(Signal signal) {
        if (signal.getTimestamp() == null) {
            signal.setTimestamp(Instant.now());
        }
        signalRepository.save(signal);
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    private void updateSignalCount(String workItemId, int count) {
        workItemRepository.findById(workItemId).ifPresent(workItem -> {
            workItem.setSignalCount(count);
            workItem.setUpdatedAt(Instant.now());
            workItemRepository.save(workItem);
        });
    }

    // Inner class representing a debounce time window
    static class DebounceWindow {
        private final String workItemId;
        private final long windowStartMs;
        private int count;

        DebounceWindow(String workItemId, long windowStartMs) {
            this.workItemId = workItemId;
            this.windowStartMs = windowStartMs;
            this.count = 1;
        }

        boolean isExpired(long nowMs) {
            return (nowMs - windowStartMs) > DEBOUNCE_WINDOW_MS;
        }

        void incrementCount() { this.count++; }
        int getCount() { return count; }
        String getWorkItemId() { return workItemId; }
    }
}
