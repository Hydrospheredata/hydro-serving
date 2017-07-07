package io.hydrosphere.serving.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.hydrosphere.serving.service.EndpointDefinition;
import io.hydrosphere.serving.service.GatewayPipelineService;
import io.hydrosphere.serving.service.HTTPGatewayServiceImpl;
import io.hydrosphere.serving.service.Pipeline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@RestController
@RequestMapping("/api/v1/serve")
public class ServingController {

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
    private GatewayPipelineService endpointService;

    @Autowired
    private HTTPGatewayServiceImpl httpGatewayService;


    @RequestMapping(value = "/{endpoint}", method = RequestMethod.POST)
    public DeferredResult<JsonNode> execute(@RequestBody JsonNode jsonNode, @PathVariable String endpoint,
                                            HttpServletRequest httpServletRequest, HttpServletResponse response) {
        Pipeline pipeline = endpointService.pipeline(endpoint)
                .orElseThrow(WrongEndpointNameException::new);

        Map<String, String> headers = new HashMap<>();
        Arrays.stream(HEADERS).forEach(s -> {
            headers.put(s, httpServletRequest.getHeader(s));
        });

        return httpGatewayService.execute(pipeline, jsonNode, headers);
    }
}
