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

/**
 *
 */
@RestController
@RequestMapping("/serve")
public class SimpleServingController {
    private static final Logger LOGGER = LogManager.getLogger();

    @Autowired
    private SideCarConfig.SideCarConfigurationProperties configurationProperties;

    @RequestMapping(method = RequestMethod.POST)
    public JsonNode serve(@RequestBody JsonNode input) {
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
        return arrayNode;
    }
}
