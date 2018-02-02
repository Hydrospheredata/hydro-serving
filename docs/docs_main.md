## Documentation

## Repository structure
* [sidecar](/sidecar) contains implementation of the sidecar pattern for ML runtimes.
* [spark-ml-serving](https://github.com/Hydrospheredata/spark-ml-serving) contains local SparkMl implementation. (Derived from Hydrosphere Mist)
* [gateway](/gateway) is a simple gateway of the whole project. For now, it's just set up Nginx.
* [manager](/manager) is a module that rules over all ML models, knows where they are, what they are.
* [runtimes and models repository](https://github.com/Hydrospheredata/hydro-serving-runtime)
    * [runtimes](https://github.com/Hydrospheredata/hydro-serving-runtime/tree/master/runtimes) contains implementations for popular ML libraries. Runtime is a small server that can import user's model and provide an HTTP API to it.
    * [models](https://github.com/Hydrospheredata/hydro-serving-runtime/tree/master/models) contains example ML models for implemented runtimes.


*WORK IN PROGRESS*