package io.hydrosphere.serving.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hydrosphere.serving.config.SideCarConfig;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 *
 */
@Service
public class HTTPGatewayServiceImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(HTTPGatewayServiceImpl.class);

    private final HttpClient httpClient = new HttpClient();

    private final SideCarConfig.SideCarConfigurationProperties properties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public HTTPGatewayServiceImpl(SideCarConfig.SideCarConfigurationProperties properties) {
        try {
            httpClient.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.properties = properties;
    }

    public DeferredResult<JsonNode> execute(EndpointDefinition definition, JsonNode jsonNode, Map<String, String> headers) {
        DeferredResult<JsonNode> r = new DeferredResult<>();
        try {
            r.setResult(sendAll(definition, jsonNode, headers));
        } catch (Exception e) {
            r.setErrorResult(e);
        }
        return r;
    }

    public JsonNode sendAll(EndpointDefinition definition, JsonNode jsonNode, Map<String, String> headers) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        JsonNode result = jsonNode;
        LOGGER.info("Request {}: {}", definition, jsonNode);
        for (String s : definition.getChain()) {
            Request request = httpClient.newRequest("http://" + properties.getHost() + ":" + properties.getPort() + s.substring(s.indexOf("/")))
                    .method(HttpMethod.POST)
                    .scheme("http")
                    .header(HttpHeader.HOST, "http-" + s.substring(0, s.indexOf("/")))
                    .content(new StringContentProvider(objectMapper.writeValueAsString(jsonNode)), "application/json");

            headers.forEach((k, v) -> {
                if (v != null && v.length() > 0) {
                    request.header(k, v);
                }
            });
            ContentResponse response = request.send();
            if (response.getStatus() == 200) {
                result = objectMapper.readTree(response.getContent());
            } else {
                LOGGER.error("SOme error na: {}, reason: {}, {}", response.getStatus(), response.getReason(), response.getContentAsString());
                throw new RuntimeException("SOme error na");
            }

            jsonNode = result;
        }
        LOGGER.info("Response {}", result);
        return result;

    }
}
