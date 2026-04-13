package com.bitirme.demo_bitirme.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables Spring's @Async support so PythonMLService can call the
 * Python FastAPI service without blocking the upload response.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
