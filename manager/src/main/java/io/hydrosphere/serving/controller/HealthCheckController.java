package io.hydrosphere.serving.controller;

import io.hydrosphere.serving.config.SideCarConfig;
import io.hydrosphere.serving.service.MeshManagerService;
import io.hydrosphere.serving.service.Service;
import io.hydrosphere.serving.service.ServiceStatus;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@Controller
@RequestMapping("/v1/health")
public class HealthCheckController {

    @Autowired
    MeshManagerService managerService;

    @Autowired
    SideCarConfig.SideCarConfigurationProperties sideCarConfigurationProperties;

    private final HttpClient httpClient = new HttpClient();

    public HealthCheckController() {
        try {
            httpClient.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @RequestMapping("/{serviceId}")
    @ResponseBody
    public DeferredResult<String> heath(@PathVariable String serviceId, HttpServletRequest httpRequest) {
        Enumeration<String> enumeration = httpRequest.getHeaderNames();
        Map<String, String> map = new HashMap<>();
        while (enumeration.hasMoreElements()) {
            String n = enumeration.nextElement();
            map.put(n, httpRequest.getHeader(n));
        }
        System.err.println(map);

        Service service = managerService.getService(serviceId);
        if (service == null) {
            throw new RuntimeException("Can't find service=" + serviceId);
        }
        DeferredResult<String> result = new DeferredResult<>();

        if (sideCarConfigurationProperties.getServiceId().equals(service.getServiceId())) {
            //TODO wrong place to do
            service.setLastKnownStatus(ServiceStatus.UP);
            result.setResult("OK");
        } else {
            httpCheck(result, service);
        }
        return result;
    }

    private void httpCheck(DeferredResult<String> result, Service service) {
        try {
            ContentResponse response = httpClient.newRequest("http://" + service.getIp() + ":" + service.getSideCarHttpPort() + "/health")
                    .method(HttpMethod.GET)
                    .scheme("http")
                    .header(HttpHeader.HOST, "http-" + service.getServiceId())
                    .send();
            if (response.getStatus() == 200) {
                //TODO wrong place to do
                service.setLastKnownStatus(ServiceStatus.UP);
                result.setResult(String.valueOf(result.getResult()));
            } else {
                //TODO wrong place to do
                service.setLastKnownStatus(ServiceStatus.DOWN);
                result.setErrorResult(response.getStatus() + " : " + String.valueOf(result.getResult()));
            }
        } catch (Exception e) {
            result.setErrorResult(e);
        }
    }
}
