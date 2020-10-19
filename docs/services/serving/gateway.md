# Gateway

{% hint style="info" %}
Gateway is an open-source service available [here](https://github.com/Hydrospheredata/hydro-serving-gateway)
as a part of [hydro-serving](https://github.com/Hydrospheredata/hydro-serving) project.
{% endhint %}

Gateway is a service responsible for routing requests to/from or between Servables
and Applications and validating these requests for matching a Model's/Application signature.

 
The Gateway maps a model’s name to a corresponding container.
Whenever it receives a request via HTTP API, GRPC, or Kafka Streams,
it communicates with that container via the gRPC protocol. 


