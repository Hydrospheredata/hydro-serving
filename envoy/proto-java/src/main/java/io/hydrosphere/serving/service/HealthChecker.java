package io.hydrosphere.serving.service;

import io.hydrosphere.serving.proto.Health;

/**
 *
 */
public interface HealthChecker {
    Health health();
}
