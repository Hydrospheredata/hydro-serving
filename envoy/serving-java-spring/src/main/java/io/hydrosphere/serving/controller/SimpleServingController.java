package io.hydrosphere.serving.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.hydrosphere.serving.service.StageExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@RestController
@RequestMapping("/v1/serving")
public class SimpleServingController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleServingController.class);

    @Autowired
    private StageExecutor emptyStageExecutor;

    @RequestMapping(method = RequestMethod.POST)
    public JsonNode serve(@RequestBody JsonNode jsonNode, HttpServletRequest httpRequest, HttpServletResponse response) {
        LOGGER.info("Request: {}", jsonNode);

        JsonNode node = emptyStageExecutor.execute("", jsonNode);
        LOGGER.info("Response: {}", jsonNode);
        return node;
    }
}
