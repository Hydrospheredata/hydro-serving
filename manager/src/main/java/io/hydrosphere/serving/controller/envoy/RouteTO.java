package io.hydrosphere.serving.controller.envoy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteTO {
    private String prefix;

    private String cluster;
}
