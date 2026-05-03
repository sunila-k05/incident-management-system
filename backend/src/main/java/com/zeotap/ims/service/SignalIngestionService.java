package com.zeotap.ims.service;

import com.zeotap.ims.engine.SignalBuffer;
import com.zeotap.ims.model.Signal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignalIngestionService {

    private final SignalBuffer signalBuffer;

    @Async
    public void ingest(Signal signal) {
        // Set timestamp if not provided
        if (signal.getTimestamp() == null) {
            signal.setTimestamp(Instant.now());
        }

        boolean accepted = signalBuffer.offer(signal);

        if (!accepted) {
            log.warn("Signal dropped - buffer full. Component: {}",
                signal.getComponentId());
        }
    }
}
