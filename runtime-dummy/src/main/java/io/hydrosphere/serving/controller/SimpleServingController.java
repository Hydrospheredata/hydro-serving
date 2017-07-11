package io.hydrosphere.serving.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.hydrosphere.serving.config.SideCarConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 *
 */
@RestController
@RequestMapping("/serve")
public class SimpleServingController {
    private static final Logger LOGGER = LogManager.getLogger();

    private final static String[] HEADERS = {
            "X-Ot-Span-Context",
            "X-Request-Id",
            "X-B3-TraceId",
            "X-B3-SpanId",
            "X-B3-ParentSpanId",
            "X-B3-Sampled",
            "X-B3-Flags"
    };

    @Autowired
    private SideCarConfig.SideCarConfigurationProperties configurationProperties;

    @RequestMapping(method = RequestMethod.POST)
    public JsonNode serve(@RequestBody JsonNode input, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        LOGGER.info("Request: {}", input);
        JsonNode jsonNode = input.deepCopy();
        ArrayNode arrayNode = (ArrayNode) jsonNode;
        for (JsonNode node : arrayNode) {
            if (node instanceof ObjectNode) {
                ObjectNode objectNode = (ObjectNode) node;
                objectNode.put(configurationProperties.getServiceId(), System.currentTimeMillis());
            }
        }
        LOGGER.info("Response: {}", arrayNode);

        Arrays.stream(HEADERS)
                .filter(s -> httpRequest.getHeaders(s) != null && httpRequest.getHeaders(s).hasMoreElements())
                .forEach(s -> httpResponse.addHeader(s, httpRequest.getHeaders(s).nextElement()));
        return arrayNode;
    }
}
