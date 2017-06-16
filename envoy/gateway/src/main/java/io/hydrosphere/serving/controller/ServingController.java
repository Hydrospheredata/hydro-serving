package io.hydrosphere.serving.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.hydrosphere.serving.proto.ServingPipeline;
import io.hydrosphere.serving.service.EndpointDefinition;
import io.hydrosphere.serving.service.EndpointService;
import io.hydrosphere.serving.service.GRPCGatewayServiceImpl;
import io.hydrosphere.serving.service.HTTPGatewayServiceImpl;
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

    private final static String[] HEADERS={
            "X-Ot-Span-Context",
            "X-Request-Id",
            "X-B3-TraceId",
            "X-B3-SpanId",
            "X-B3-ParentSpanId",
            "X-B3-Sampled",
            "X-B3-Flags"
    };

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private GRPCGatewayServiceImpl grpcGatewayService;

    @Autowired
    private HTTPGatewayServiceImpl httpGatewayService;


    @RequestMapping(value = "/{endpoint}", method = RequestMethod.POST)
    public DeferredResult<JsonNode> execute(@RequestBody JsonNode jsonNode, @PathVariable String endpoint,
                                            HttpServletRequest httpServletRequest, HttpServletResponse response) {
        EndpointDefinition definition = endpointService.endpointDefinition(endpoint);
        if (definition == null) {
            throw new WrongEndpointNameException();
        }

        Map<String, String> headers = new HashMap<>();
        Arrays.stream(HEADERS).forEach(s->{
            headers.put(s, httpServletRequest.getHeader(s));
        });
        if ("http".equals(definition.getTransportType())) {
            return httpGatewayService.execute(definition, jsonNode, headers);
        } else {
            ServingPipeline servingPipeline = endpointService.getPipeline(endpoint, jsonNode);
            return grpcGatewayService.sendToMesh(servingPipeline, headers);
        }
    }
}
