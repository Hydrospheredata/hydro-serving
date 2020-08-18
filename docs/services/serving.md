# Serving

## Gateway

Gateway is a service responsible for routing requests to/from or between Servables and Applications and validating these requests for matching a Model's/Application signature.

The Gateway maps a model’s name to a corresponding container. Whenever it receives a request via HTTP API, GRPC, or Kafka Streams, it communicates with that container via the gRPC protocol.

## Manager

Manager is responsible for:

* Building a Docker Image from your ML model for future deployment
* Storing these images inside a Docker Registry deployed alongside with

  manager service

* Versioning these images as Model Versions
* Creating running instances of these Model Versions called Servables

  inside Kubernetes cluster

* Combining multiple Model Versions into a linear graph with a single

  endpoint called Application



