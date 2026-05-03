package com.zeotap.ims.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
public class RetryConfig {
    // Enables @Retryable annotations throughout the application
    // Used in DebounceEngine for DB write retry with exponential backoff
}
