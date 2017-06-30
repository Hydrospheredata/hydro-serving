package io.hydrosphere.serving.controller;

import io.hydrosphere.serving.service.MeshManagerService;
import io.hydrosphere.serving.service.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 *
 */
@RestController
@RequestMapping("/api/v1/runtime")
public class RuntimeController {

    @Autowired
    private MeshManagerService managerService;

    @RequestMapping(method = RequestMethod.GET)
    public List<Service> list() {
        return managerService.getRuntimes();
    }
}
