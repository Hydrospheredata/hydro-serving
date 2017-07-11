#### UI

#### Manager
* Pipeline validation: on create, on start
* shadowing for container/pipeline
* canary for container/pipeline
* blue-green for container/pipeline
* split pipeline and endpoint
* test one service
* service scaling

#### Gateway
* pipeline logic
* Tracing initialization
* split pipeline and endpoint

#### Sidecar
* move sidecar to separate repository with custom modules https://github.com/lyft/envoy-filter-example
* Add metrics
    1. Add prometheus endpoint to sidecar admin
        * Add /stats_prom endpoint to envoy
        * Add integration with Manager to prometheus https://prometheus.io/blog/2015/06/01/advanced-service-discovery/#custom-service-discovery
    2. Add metrics pull to manager
        * add metrics pulling logic to Manager
        * add metrics pushing to prometheus/graphite/etc.
* Unix domain socket for local communication between service and Envoy
    * Add socket support to envoy (or wait for istio https://istio.io/docs/concepts/network-and-auth/auth.html#future-work)
    * Add UDS support to models in java/python
    * Create UDS client for java/python for accessing another models
* Add pipeline support
    * errors handling
* logs collecting