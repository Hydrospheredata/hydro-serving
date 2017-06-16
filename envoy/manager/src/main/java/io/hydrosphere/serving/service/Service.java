package io.hydrosphere.serving.service;

import lombok.Data;
import lombok.ToString;

/**
 *
 */
@Data
@ToString
public class Service {
    private String hostIp;

    private String ip;

    private Integer sideCarAdminPort;

    private Integer sideCarGrpcPort;

    private Integer sideCarHttpPort;

    private Integer serviceHttpPort;

    private Integer serviceGrpcPort;

    private String serviceName;

    private String serviceVersion;

    private String serviceId;

    private String serviceUUID;

    private ServiceType serviceType;

    private boolean useServiceHttp;

    private boolean useServiceGrpc;

    private ServiceStatus lastKnownStatus;
}
